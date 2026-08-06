package com.metaforge.metadata.application.service;

import com.metaforge.metadata.api.dto.request.CreateDraftRequest;
import com.metaforge.metadata.api.dto.request.UpdateDraftContentRequest;
import com.metaforge.metadata.api.dto.response.MetadataEntityDraftDto;
import com.metaforge.metadata.api.service.MetadataDraftService;
import com.metaforge.metadata.domain.exception.DraftNotFoundException;
import com.metaforge.metadata.domain.exception.EntityNotFoundException;
import com.metaforge.metadata.domain.exception.FqnConflictException;
import com.metaforge.metadata.domain.model.aggregate.MetadataEntity;
import com.metaforge.metadata.domain.model.aggregate.MetadataEntityDraft;
import com.metaforge.metadata.domain.model.valueobject.EntitySchemaFQN;
import com.metaforge.metadata.domain.model.valueobject.FQN;
import com.metaforge.metadata.domain.repository.EntitySchemaRepository;
import com.metaforge.metadata.domain.repository.MetadataEntityDraftRepository;
import com.metaforge.metadata.domain.repository.MetadataEntityRepository;
import com.metaforge.metadata.domain.service.FqnGenerator;
import com.metaforge.metadata.domain.service.FqnUniquenessService;
import com.metaforge.metadata.domain.service.SchemaValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@Transactional
public class MetadataDraftServiceImpl implements MetadataDraftService {

    private static final Logger log = LoggerFactory.getLogger(MetadataDraftServiceImpl.class);

    private final MetadataEntityDraftRepository draftRepository;
    private final MetadataEntityRepository entityRepository;
    private final FqnGenerator fqnGenerator;
    private final FqnUniquenessService fqnUniquenessService;
    private final SchemaValidationService schemaValidationService;
    private final EntitySchemaRepository entitySchemaRepository;

    public MetadataDraftServiceImpl(MetadataEntityDraftRepository draftRepository,
                                    MetadataEntityRepository entityRepository,
                                    FqnGenerator fqnGenerator,
                                    FqnUniquenessService fqnUniquenessService,
                                    SchemaValidationService schemaValidationService,
                                    EntitySchemaRepository entitySchemaRepository) {
        this.draftRepository = draftRepository;
        this.entityRepository = entityRepository;
        this.fqnGenerator = fqnGenerator;
        this.fqnUniquenessService = fqnUniquenessService;
        this.schemaValidationService = schemaValidationService;
        this.entitySchemaRepository = entitySchemaRepository;
    }

    @Override
    public MetadataEntityDraftDto createDraft(CreateDraftRequest request) {
        log.info("创建草稿: fqn={}, entitySchemaFqn={}", request.getFqn(), request.getEntitySchemaFqn());

        validateCreateDraftRequest(request);

        String createdBy = request.getCreatedBy() != null ? request.getCreatedBy() : "system";
        MetadataEntityDraft draft = new MetadataEntityDraft(
                FQN.of(request.getFqn()),
                request.getName(),
                request.getDescription(),
                request.getParentFqn(),
                EntitySchemaFQN.of(request.getEntitySchemaFqn()),
                request.getContent(),
                null,
                createdBy);

        MetadataEntityDraft saved = draftRepository.save(draft);
        log.info("草稿创建成功: fqn={}, id={}", saved.getFqnValue(), saved.getId());
        return toDraftDto(saved);
    }

    @Override
    public MetadataEntityDraftDto createDraftFromActive(String fqn, String createdBy) {
        log.info("从生效版本创建修改草稿: fqn={}", fqn);

        MetadataEntity entity = entityRepository.findByFqn(fqn)
                .orElseThrow(() -> new EntityNotFoundException("生效元数据不存在: " + fqn));

        if (draftRepository.existsByFqn(fqn)) {
            throw new FqnConflictException("该 FQN 已存在草稿: " + fqn);
        }

        String operator = createdBy != null ? createdBy : "system";
        MetadataEntityDraft draft = new MetadataEntityDraft(
                entity.getFqn(),
                entity.getName(),
                entity.getDescription(),
                entity.getParentFqn(),
                entity.getEntitySchemaFqn(),
                entity.getContent(),
                entity.getCurrentVersionValue(),
                operator);

        MetadataEntityDraft saved = draftRepository.save(draft);
        log.info("从生效版本创建草稿成功: fqn={}, baseVersion={}", saved.getFqnValue(), saved.getBaseVersion());
        return toDraftDto(saved);
    }

    @Override
    public MetadataEntityDraftDto updateDraftContent(String fqn, UpdateDraftContentRequest request) {
        log.info("更新草稿内容: fqn={}", fqn);

        MetadataEntityDraft draft = draftRepository.findByFqn(fqn)
                .orElseThrow(() -> new DraftNotFoundException("草稿不存在: " + fqn));

        Map<String, Object> newContent = request.getContent();
        if (newContent != null) {
            schemaValidationService.validate(draft.getEntitySchemaFqnValue(), newContent);
        }

        String updatedBy = request.getUpdatedBy() != null ? request.getUpdatedBy() : "system";
        draft.updateContent(newContent, updatedBy);

        MetadataEntityDraft saved = draftRepository.save(draft);
        log.info("草稿内容更新成功: fqn={}", saved.getFqnValue());
        return toDraftDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MetadataEntityDraftDto getDraft(String fqn) {
        log.debug("查询草稿: fqn={}", fqn);
        MetadataEntityDraft draft = draftRepository.findByFqn(fqn)
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
        String fqn = request.getFqn();

        for (String segment : fqnGenerator.splitSegments(fqn)) {
            if (fqnGenerator.isReservedCharInSegment(segment)) {
                throw new FqnConflictException(
                        "FQN segment 包含保留分隔符 '.': " + segment);
            }
            if (!fqnGenerator.isValidSegment(segment)) {
                throw new FqnConflictException(
                        "FQN segment 不符合文法 [A-Za-z][A-Za-z0-9_-]*: " + segment);
            }
        }

        if (!fqnUniquenessService.isFqnUnique(fqn)) {
            throw new FqnConflictException("FQN 已存在（主表或草稿表）: " + fqn);
        }

        log.debug("元模型版本发布状态校验（上游适配器占位）: {}", request.getEntitySchemaFqn());

        String parentFqn = request.getParentFqn();
        if (parentFqn != null && !parentFqn.isEmpty()) {
            if (!entityRepository.existsByFqn(parentFqn)) {
                throw new FqnConflictException("父实体未生效或不存在: " + parentFqn);
            }
        }

        schemaValidationService.validate(request.getEntitySchemaFqn(), request.getContent());
    }

    private MetadataEntityDraftDto toDraftDto(MetadataEntityDraft draft) {
        MetadataEntityDraftDto dto = new MetadataEntityDraftDto();
        dto.setId(draft.getId());
        dto.setFqn(draft.getFqnValue());
        dto.setName(draft.getName());
        dto.setDescription(draft.getDescription());
        dto.setParentFqn(draft.getParentFqn());
        dto.setEntitySchemaFqn(draft.getEntitySchemaFqnValue());
        dto.setContent(draft.getContent());
        dto.setBaseVersion(draft.getBaseVersion());
        dto.setCreatedBy(draft.getCreatedBy());
        dto.setCreatedTime(draft.getCreatedTime());
        dto.setUpdatedBy(draft.getUpdatedBy());
        dto.setUpdatedTime(draft.getUpdatedTime());
        return dto;
    }
}
