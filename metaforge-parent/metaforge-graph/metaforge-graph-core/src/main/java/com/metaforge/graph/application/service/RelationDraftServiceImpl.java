package com.metaforge.graph.application.service;

import com.metaforge.graph.api.constant.GraphErrorCode;
import com.metaforge.graph.api.dto.CreateDraftRequest;
import com.metaforge.graph.api.dto.RelationInstanceDraftDto;
import com.metaforge.graph.api.dto.UpdateDraftContentRequest;
import com.metaforge.graph.api.service.RelationDraftService;
import com.metaforge.graph.infrastructure.config.GraphBizException;
import com.metaforge.graph.domain.model.aggregate.RelationInstance;
import com.metaforge.graph.domain.model.aggregate.RelationInstanceDraft;
import com.metaforge.graph.domain.model.valueobject.EntityFQN;
import com.metaforge.graph.domain.model.valueobject.FQN;
import com.metaforge.graph.domain.model.valueobject.RelationDescription;
import com.metaforge.graph.domain.model.valueobject.RelationName;
import com.metaforge.graph.domain.model.valueobject.RelationSchemaFQN;
import com.metaforge.graph.domain.repository.MetadataEntityGateway;
import com.metaforge.graph.domain.repository.RelationInstanceDraftRepository;
import com.metaforge.graph.domain.repository.RelationInstanceRepository;
import com.metaforge.graph.domain.repository.RelationSchemaRepository;
import com.metaforge.graph.domain.service.CardinalityValidationService;
import com.metaforge.graph.domain.service.FqnGenerator;
import com.metaforge.graph.domain.service.RelationSchemaValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 关系实例草稿管理服务实现。
 */
@Service
@Transactional
public class RelationDraftServiceImpl implements RelationDraftService {

    private static final Logger log = LoggerFactory.getLogger(RelationDraftServiceImpl.class);

    private final RelationInstanceDraftRepository draftRepository;
    private final RelationInstanceRepository instanceRepository;
    private final RelationSchemaRepository relationSchemaRepository;
    private final MetadataEntityGateway metadataEntityGateway;
    private final RelationSchemaValidationService schemaValidationService;
    private final CardinalityValidationService cardinalityValidationService;

    public RelationDraftServiceImpl(RelationInstanceDraftRepository draftRepository,
                                     RelationInstanceRepository instanceRepository,
                                     RelationSchemaRepository relationSchemaRepository,
                                     MetadataEntityGateway metadataEntityGateway,
                                     RelationSchemaValidationService schemaValidationService,
                                     CardinalityValidationService cardinalityValidationService) {
        this.draftRepository = draftRepository;
        this.instanceRepository = instanceRepository;
        this.relationSchemaRepository = relationSchemaRepository;
        this.metadataEntityGateway = metadataEntityGateway;
        this.schemaValidationService = schemaValidationService;
        this.cardinalityValidationService = cardinalityValidationService;
    }

    @Override
    public RelationInstanceDraftDto createDraft(CreateDraftRequest request) {
        log.info("创建草稿: source={}, typeFqn={}, target={}",
                request.getSourceEntityFqn(), request.getRelationTypeFqn(), request.getTargetEntityFqn());

        validateCreateDraftRequest(request);

        EntityFQN sourceFqn = EntityFQN.of(request.getSourceEntityFqn());
        EntityFQN targetFqn = EntityFQN.of(request.getTargetEntityFqn());

        FQN fqn = FqnGenerator.generate(sourceFqn, request.getRelationTypeFqn(), targetFqn);

        if (draftRepository.existsByFqn(fqn.getValue()) || instanceRepository.existsByFqnString(fqn.getValue())) {
            throw new FqnConflictException("FQN 已存在: " + fqn.getValue());
        }

        String relationType = extractRelationType(request.getRelationTypeFqn());

        RelationInstanceDraft draft = new RelationInstanceDraft();
        draft.setFqn(fqn);
        draft.setName(RelationName.of(request.getName()));
        draft.setDescription(RelationDescription.of(request.getDescription()));
        draft.setSourceEntityFqn(sourceFqn);
        draft.setTargetEntityFqn(targetFqn);
        draft.setRelationType(relationType);
        draft.setRelationSchemaFqn(RelationSchemaFQN.of(request.getRelationTypeFqn()));
        draft.setContent(request.getContent());
        draft.setEmbedding(request.getEmbedding());
        draft.setCreatedBy("system");

        RelationInstanceDraft saved = draftRepository.save(draft);
        log.info("草稿创建成功: fqn={}", saved.getFqnValue());

        return toDraftDto(saved);
    }

    @Override
    public RelationInstanceDraftDto createDraftFromActive(String fqn) {
        log.info("基于生效版本创建草稿: fqn={}", fqn);

        RelationInstance active = instanceRepository.findByFqnString(fqn)
                .orElseThrow(() -> new RelationNotFoundException("生效关系不存在: " + fqn));

        if (draftRepository.existsByFqn(fqn)) {
            throw new DraftAlreadyExistsException("该 FQN 已存在草稿: " + fqn);
        }

        RelationInstanceDraft draft = new RelationInstanceDraft();
        draft.setFqn(active.getFqn());
        draft.setName(active.getName());
        draft.setDescription(active.getDescription());
        draft.setSourceEntityFqn(active.getSourceEntityFqn());
        draft.setTargetEntityFqn(active.getTargetEntityFqn());
        draft.setRelationType(active.getRelationType());
        draft.setRelationSchemaFqn(active.getRelationSchemaFqn());
        draft.setContent(active.getContent());
        draft.setEmbedding(active.getEmbedding());
        draft.setBaseVersion(active.getCurrentVersionValue());
        draft.setCreatedBy("system");

        RelationInstanceDraft saved = draftRepository.save(draft);
        log.info("基于生效版本创建草稿成功: fqn={}", saved.getFqnValue());

        return toDraftDto(saved);
    }

    @Override
    public RelationInstanceDraftDto updateDraftContent(String fqn, UpdateDraftContentRequest request) {
        log.info("更新草稿内容: fqn={}", fqn);

        RelationInstanceDraft draft = draftRepository.findByFqn(fqn)
                .orElseThrow(() -> new DraftNotFoundException("草稿不存在: " + fqn));

        schemaValidationService.validate(draft.getRelationSchemaFqnValue(), request.getContent());
        draft.updateContent(request.getContent(), request.getEmbedding());
        RelationInstanceDraft saved = draftRepository.save(draft);

        log.info("草稿内容更新成功: fqn={}", fqn);
        return toDraftDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RelationInstanceDraftDto getDraft(String fqn) {
        log.debug("查询草稿: fqn={}", fqn);
        RelationInstanceDraft draft = draftRepository.findByFqn(fqn)
                .orElseThrow(() -> new DraftNotFoundException("草稿不存在: " + fqn));
        return toDraftDto(draft);
    }

    @Override
    public void deleteDraft(String fqn) {
        log.info("删除草稿: fqn={}", fqn);
        if (!draftRepository.existsByFqn(fqn)) {
            throw new DraftNotFoundException("草稿不存在: " + fqn);
        }
        draftRepository.deleteByFqn(fqn);
        log.info("草稿删除成功: fqn={}", fqn);
    }

    private void validateCreateDraftRequest(CreateDraftRequest request) {
        if (!relationSchemaRepository.isSchemaPublished(request.getRelationTypeFqn())) {
            throw new SchemaNotPublishedException(
                    "RelationSchema 版本未发布: " + request.getRelationTypeFqn());
        }

        if (!metadataEntityGateway.isEntityActive(request.getSourceEntityFqn())) {
            throw new EndpointNotActiveException(
                    "源端实体未生效: " + request.getSourceEntityFqn());
        }

        if (!metadataEntityGateway.isEntityActive(request.getTargetEntityFqn())) {
            throw new EndpointNotActiveException(
                    "目标端实体未生效: " + request.getTargetEntityFqn());
        }

        schemaValidationService.validate(request.getRelationTypeFqn(), request.getContent());
    }

    /**
     * 从关系类型 FQN 中提取类型名（最后一段）。
     */
    private String extractRelationType(String relationTypeFqn) {
        if (relationTypeFqn == null || relationTypeFqn.isEmpty()) {
            return relationTypeFqn;
        }
        int lastSeparator = Math.max(
                relationTypeFqn.lastIndexOf('.'),
                relationTypeFqn.lastIndexOf(':')
        );
        return lastSeparator >= 0 ? relationTypeFqn.substring(lastSeparator + 1) : relationTypeFqn;
    }

    private RelationInstanceDraftDto toDraftDto(RelationInstanceDraft draft) {
        RelationInstanceDraftDto dto = new RelationInstanceDraftDto();
        dto.setId(draft.getId());
        dto.setFqn(draft.getFqnValue());
        dto.setName(draft.getNameValue());
        dto.setDescription(draft.getDescriptionValue());
        dto.setSourceEntityFqn(draft.getSourceEntityFqnValue());
        dto.setTargetEntityFqn(draft.getTargetEntityFqnValue());
        dto.setRelationType(draft.getRelationType());
        dto.setRelationSchemaFqn(draft.getRelationSchemaFqnValue());
        dto.setContent(draft.getContent());
        dto.setEmbedding(draft.getEmbedding());
        dto.setBaseVersion(draft.getBaseVersion());
        dto.setCreatedBy(draft.getCreatedBy());
        dto.setCreatedTime(draft.getCreatedTime());
        dto.setUpdatedBy(draft.getUpdatedBy());
        dto.setUpdatedTime(draft.getUpdatedTime());
        return dto;
    }

    // ---- 业务异常 ----

    public static class FqnConflictException extends GraphBizException {
        public FqnConflictException(String message) {
            super(GraphErrorCode.FQN_CONFLICT, message);
        }
        @Override
        public String getErrorCodeName() { return "FQN_CONFLICT"; }
    }

    public static class RelationNotFoundException extends GraphBizException {
        public RelationNotFoundException(String message) {
            super(GraphErrorCode.RELATION_NOT_FOUND, message);
        }
        @Override
        public String getErrorCodeName() { return "RELATION_NOT_FOUND"; }
    }

    public static class DraftNotFoundException extends GraphBizException {
        public DraftNotFoundException(String message) {
            super(GraphErrorCode.DRAFT_NOT_FOUND, message);
        }
        @Override
        public String getErrorCodeName() { return "DRAFT_NOT_FOUND"; }
    }

    public static class DraftAlreadyExistsException extends GraphBizException {
        public DraftAlreadyExistsException(String message) {
            super(GraphErrorCode.DRAFT_ALREADY_EXISTS, message);
        }
        @Override
        public String getErrorCodeName() { return "DRAFT_ALREADY_EXISTS"; }
    }

    public static class SchemaNotPublishedException extends GraphBizException {
        public SchemaNotPublishedException(String message) {
            super(GraphErrorCode.SCHEMA_NOT_PUBLISHED, message);
        }
        @Override
        public String getErrorCodeName() { return "SCHEMA_NOT_PUBLISHED"; }
    }

    public static class EndpointNotActiveException extends GraphBizException {
        public EndpointNotActiveException(String message) {
            super(GraphErrorCode.ENDPOINT_INVALID, message);
        }
        @Override
        public String getErrorCodeName() { return "ENDPOINT_INVALID"; }
    }
}
