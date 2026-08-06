package com.metaforge.graph.application.service;

import com.metaforge.graph.api.constant.GraphConstants;
import com.metaforge.graph.api.constant.GraphErrorCode;
import com.metaforge.graph.api.dto.DeactivationCheckResult;
import com.metaforge.graph.api.dto.RelationInstanceDto;
import com.metaforge.graph.api.enums.ChangeType;
import com.metaforge.graph.api.event.RelationChangeEvent;
import com.metaforge.graph.api.service.RelationActivationService;
import com.metaforge.graph.infrastructure.config.GraphBizException;
import com.metaforge.graph.domain.event.RelationEventPublisher;
import com.metaforge.graph.domain.model.aggregate.RelationInstance;
import com.metaforge.graph.domain.model.aggregate.RelationInstanceDraft;
import com.metaforge.graph.domain.model.aggregate.RelationVersion;
import com.metaforge.graph.domain.model.valueobject.*;
import com.metaforge.graph.domain.repository.RelationInstanceDraftRepository;
import com.metaforge.graph.domain.repository.RelationInstanceRepository;
import com.metaforge.graph.domain.repository.RelationSchemaRepository;
import com.metaforge.graph.domain.repository.RelationVersionRepository;
import com.metaforge.graph.domain.service.CardinalityValidationService;
import com.metaforge.graph.domain.service.DependencyCheckService;
import com.metaforge.graph.domain.service.RelationSchemaValidationService;
import com.metaforge.graph.infrastructure.persistence.jpa.EntityRelationIndexJpaRepository;
import com.metaforge.graph.infrastructure.persistence.jpa.EntityRelationIndexJpo;
import com.metaforge.graph.infrastructure.persistence.jpa.RelationInstanceJpo;
import com.metaforge.graph.infrastructure.persistence.jpa.RelationInstanceJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 关系实例版本生效与生命周期管控服务实现。
 */
@Service
@Transactional
public class RelationActivationServiceImpl implements RelationActivationService {

    private static final Logger log = LoggerFactory.getLogger(RelationActivationServiceImpl.class);

    private final RelationInstanceDraftRepository draftRepository;
    private final RelationInstanceRepository instanceRepository;
    private final RelationVersionRepository versionRepository;
    private final RelationInstanceJpaRepository instanceJpaRepository;
    private final EntityRelationIndexJpaRepository indexJpaRepository;
    private final RelationSchemaValidationService schemaValidationService;
    private final RelationEventPublisher eventPublisher;
    private final DependencyCheckService dependencyCheckService;
    private final CardinalityValidationService cardinalityValidationService;
    private final RelationSchemaRepository relationSchemaRepository;

    public RelationActivationServiceImpl(RelationInstanceDraftRepository draftRepository,
                                          RelationInstanceRepository instanceRepository,
                                          RelationVersionRepository versionRepository,
                                          RelationInstanceJpaRepository instanceJpaRepository,
                                          EntityRelationIndexJpaRepository indexJpaRepository,
                                          RelationSchemaValidationService schemaValidationService,
                                          RelationEventPublisher eventPublisher,
                                          DependencyCheckService dependencyCheckService,
                                          CardinalityValidationService cardinalityValidationService,
                                          RelationSchemaRepository relationSchemaRepository) {
        this.draftRepository = draftRepository;
        this.instanceRepository = instanceRepository;
        this.versionRepository = versionRepository;
        this.instanceJpaRepository = instanceJpaRepository;
        this.indexJpaRepository = indexJpaRepository;
        this.schemaValidationService = schemaValidationService;
        this.eventPublisher = eventPublisher;
        this.dependencyCheckService = dependencyCheckService;
        this.cardinalityValidationService = cardinalityValidationService;
        this.relationSchemaRepository = relationSchemaRepository;
    }

    @Override
    public RelationInstanceDto activate(String fqn) {
        log.info("执行草稿生效: fqn={}", fqn);

        RelationInstanceDraft draft = draftRepository.findByFqn(fqn)
                .orElseThrow(() -> new DraftNotFoundException("草稿不存在: " + fqn));

        schemaValidationService.validate(draft.getRelationSchemaFqnValue(), draft.getContent());

        cardinalityValidationService.validate(
                draft.getRelationType(),
                draft.getSourceEntityFqnValue(),
                draft.getTargetEntityFqnValue(),
                relationSchemaRepository.getCardinalityRule(draft.getRelationSchemaFqnValue()),
                instanceJpaRepository.findByFqn(fqn).map(RelationInstanceJpo::getFqn).orElse(null));

        RelationInstance instance = RelationInstance.fromDraft(draft);

        int nextVersion = nextVersionNumber(fqn);
        if (instanceJpaRepository.findByFqn(fqn).isPresent()) {
            RelationInstanceJpo existing = instanceJpaRepository.findByFqn(fqn).get();
            instance.setId(existing.getId());
            instance.setCreatedTime(existing.getCreatedTime());
            instance.setCurrentVersion(VersionNumber.of(nextVersion));
        } else {
            instance.setCreatedTime(LocalDateTime.now());
            instance.setCurrentVersion(VersionNumber.of(nextVersion));
        }
        instance.setUpdatedTime(LocalDateTime.now());

        RelationInstance savedInstance = instanceRepository.save(instance);

        RelationVersion version = new RelationVersion();
        version.setFqn(savedInstance.getFqnValue());
        version.setName(savedInstance.getNameValue());
        version.setDescription(savedInstance.getDescriptionValue());
        version.setSourceEntityFqn(savedInstance.getSourceEntityFqnValue());
        version.setTargetEntityFqn(savedInstance.getTargetEntityFqnValue());
        version.setRelationType(savedInstance.getRelationType());
        version.setRelationSchemaFqn(savedInstance.getRelationSchemaFqnValue());
        version.setContent(savedInstance.getContent() != null
                ? com.metaforge.common.util.JsonbUtils.toJsonb(savedInstance.getContent()) : null);
        version.setEmbedding(savedInstance.getEmbedding() != null
                ? com.metaforge.common.util.JsonbUtils.toJsonb(savedInstance.getEmbedding()) : null);
        version.setVersion(savedInstance.getCurrentVersionValue());
        version.setActivatedBy("system");
        versionRepository.save(version);

        draftRepository.deleteByFqn(fqn);

        updateBidirectionalIndex(savedInstance);

        log.info("草稿生效成功: fqn={}, version={}", fqn, savedInstance.getCurrentVersionValue());

        eventPublisher.publishActivated(new RelationChangeEvent(
                this, savedInstance.getFqnValue(), ChangeType.ACTIVATED,
                savedInstance.getCurrentVersionValue(),
                savedInstance.getRelationSchemaFqnValue(),
                savedInstance.getSourceEntityFqnValue(),
                savedInstance.getTargetEntityFqnValue()));

        return toInstanceDto(savedInstance);
    }

    @Override
    public void deprecate(String fqn) {
        log.info("执行关系下线: fqn={}", fqn);

        RelationInstance instance = instanceRepository.findByFqnString(fqn)
                .orElseThrow(() -> new RelationNotFoundException("生效关系不存在: " + fqn));

        DeactivationCheckResult checkResult = checkDeactivationPreconditions(fqn);
        if (!checkResult.isCanDeprecate()) {
            throw new DependencyBlockedException(
                    "存在下游强依赖，阻塞下线: " + String.join(", ", checkResult.getBlockingRelations()));
        }

        indexJpaRepository.deleteByRelationFqn(fqn);
        instanceRepository.deleteByFqn(instance.getFqn());

        log.info("关系下线成功: fqn={}", fqn);

        eventPublisher.publishDeprecated(new RelationChangeEvent(
                this, fqn, ChangeType.DEPRECATED,
                instance.getCurrentVersionValue(),
                instance.getRelationSchemaFqnValue(),
                instance.getSourceEntityFqnValue(),
                instance.getTargetEntityFqnValue()));
    }

    @Override
    public RelationInstanceDto reactivate(String fqn) {
        log.info("重新生效: fqn={}", fqn);

        if (instanceRepository.existsByFqnString(fqn)) {
            throw new RelationAlreadyActiveException("关系已处于生效状态，无需重新生效: " + fqn);
        }

        List<RelationVersion> versions = versionRepository.findByFqnOrderByVersionDesc(fqn);
        if (versions.isEmpty()) {
            throw new VersionNotFoundException("历史版本不存在: " + fqn);
        }

        RelationVersion latestVersion = versions.get(0);

        RelationInstance instance = new RelationInstance();
        instance.setFqn(FQN.of(latestVersion.getFqn()));
        instance.setName(RelationName.of(latestVersion.getName()));
        instance.setDescription(RelationDescription.of(latestVersion.getDescription()));
        instance.setSourceEntityFqn(EntityFQN.of(latestVersion.getSourceEntityFqn()));
        instance.setTargetEntityFqn(EntityFQN.of(latestVersion.getTargetEntityFqn()));
        instance.setRelationType(latestVersion.getRelationType());
        instance.setRelationSchemaFqn(RelationSchemaFQN.of(latestVersion.getRelationSchemaFqn()));
        instance.setContent(parseContent(latestVersion.getContent()));
        instance.setEmbedding(parseEmbedding(latestVersion.getEmbedding()));
        instance.setCurrentVersion(VersionNumber.of(latestVersion.getVersion()));
        instance.setCreatedTime(latestVersion.getActivatedTime());
        instance.setUpdatedTime(LocalDateTime.now());

        RelationInstance savedInstance = instanceRepository.save(instance);

        updateBidirectionalIndex(savedInstance);

        log.info("重新生效成功: fqn={}", fqn);
        return toInstanceDto(savedInstance);
    }

    private Map<String, Object> parseContent(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        return com.metaforge.common.util.JsonbUtils.fromJsonb(json, Map.class);
    }

    private int nextVersionNumber(String fqn) {
        int maxVersion = 0;
        List<RelationVersion> versions = versionRepository.findByFqnOrderByVersionDesc(fqn);
        if (!versions.isEmpty()) {
            maxVersion = versions.get(0).getVersion();
        }
        return maxVersion + 1;
    }

    private List<Float> parseEmbedding(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        return com.metaforge.common.util.JsonbUtils.fromJsonb(json, List.class);
    }

    @Override
    @Transactional(readOnly = true)
    public DeactivationCheckResult checkDeactivationPreconditions(String fqn) {
        List<String> blocking = dependencyCheckService.findBlockingDependencies(fqn);
        if (!blocking.isEmpty()) {
            return DeactivationCheckResult.blocked(blocking);
        }
        return DeactivationCheckResult.pass();
    }

    private void updateBidirectionalIndex(RelationInstance instance) {
        indexJpaRepository.deleteByRelationFqn(instance.getFqnValue());

        EntityRelationIndexJpo outbound = new EntityRelationIndexJpo();
        outbound.setEntityFqn(instance.getSourceEntityFqnValue());
        outbound.setDirection(GraphConstants.DIRECTION_OUTBOUND);
        outbound.setRelationFqn(instance.getFqnValue());
        indexJpaRepository.save(outbound);

        EntityRelationIndexJpo inbound = new EntityRelationIndexJpo();
        inbound.setEntityFqn(instance.getTargetEntityFqnValue());
        inbound.setDirection(GraphConstants.DIRECTION_INBOUND);
        inbound.setRelationFqn(instance.getFqnValue());
        indexJpaRepository.save(inbound);
    }

    private RelationInstanceDto toInstanceDto(RelationInstance instance) {
        RelationInstanceDto dto = new RelationInstanceDto();
        dto.setId(instance.getId());
        dto.setFqn(instance.getFqnValue());
        dto.setName(instance.getNameValue());
        dto.setDescription(instance.getDescriptionValue());
        dto.setSourceEntityFqn(instance.getSourceEntityFqnValue());
        dto.setTargetEntityFqn(instance.getTargetEntityFqnValue());
        dto.setRelationType(instance.getRelationType());
        dto.setRelationSchemaFqn(instance.getRelationSchemaFqnValue());
        dto.setContent(instance.getContent());
        dto.setEmbedding(instance.getEmbedding());
        dto.setCurrentVersion(instance.getCurrentVersionValue());
        dto.setCreatedBy(instance.getCreatedBy());
        dto.setCreatedTime(instance.getCreatedTime());
        dto.setUpdatedBy(instance.getUpdatedBy());
        dto.setUpdatedTime(instance.getUpdatedTime());
        return dto;
    }

    // ---- 业务异常 ----

    public static class DraftNotFoundException extends GraphBizException {
        public DraftNotFoundException(String message) {
            super(GraphErrorCode.DRAFT_NOT_FOUND, message);
        }
        @Override public String getErrorCodeName() { return "DRAFT_NOT_FOUND"; }
    }

    public static class RelationNotFoundException extends GraphBizException {
        public RelationNotFoundException(String message) {
            super(GraphErrorCode.RELATION_NOT_FOUND, message);
        }
        @Override public String getErrorCodeName() { return "RELATION_NOT_FOUND"; }
    }

    public static class DependencyBlockedException extends GraphBizException {
        public DependencyBlockedException(String message) {
            super(GraphErrorCode.DEPENDENCY_BLOCKING, message);
        }
        @Override public String getErrorCodeName() { return "DEPENDENCY_BLOCKING"; }
    }

    public static class VersionNotFoundException extends GraphBizException {
        public VersionNotFoundException(String message) {
            super(GraphErrorCode.VERSION_NOT_FOUND, message);
        }
        @Override public String getErrorCodeName() { return "VERSION_NOT_FOUND"; }
    }

    public static class RelationAlreadyActiveException extends GraphBizException {
        public RelationAlreadyActiveException(String message) {
            super(GraphErrorCode.ILLEGAL_STATE, message);
        }
        @Override public String getErrorCodeName() { return "ILLEGAL_STATE"; }
    }
}
