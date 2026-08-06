package com.metaforge.metadata.domain.service;

import com.metaforge.metadata.domain.exception.ActivationFailedException;
import com.metaforge.metadata.domain.exception.DraftNotFoundException;
import com.metaforge.metadata.domain.model.aggregate.MetadataEntity;
import com.metaforge.metadata.domain.model.aggregate.MetadataEntityDraft;
import com.metaforge.metadata.domain.model.entity.EntityVersion;
import com.metaforge.metadata.domain.model.valueobject.VersionNumber;
import com.metaforge.metadata.domain.repository.EntitySchemaRepository;
import com.metaforge.metadata.domain.repository.EntityVersionRepository;
import com.metaforge.metadata.domain.repository.MetadataEntityDraftRepository;
import com.metaforge.metadata.domain.repository.MetadataEntityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DraftActivationService {

    private static final Logger log = LoggerFactory.getLogger(DraftActivationService.class);

    private final MetadataEntityDraftRepository draftRepository;
    private final MetadataEntityRepository entityRepository;
    private final EntityVersionRepository versionRepository;
    private final SchemaValidationService schemaValidationService;
    private final EntitySchemaRepository entitySchemaRepository;

    public DraftActivationService(MetadataEntityDraftRepository draftRepository,
                                  MetadataEntityRepository entityRepository,
                                  EntityVersionRepository versionRepository,
                                  SchemaValidationService schemaValidationService,
                                  EntitySchemaRepository entitySchemaRepository) {
        this.draftRepository = draftRepository;
        this.entityRepository = entityRepository;
        this.versionRepository = versionRepository;
        this.schemaValidationService = schemaValidationService;
        this.entitySchemaRepository = entitySchemaRepository;
    }

    @Transactional
    public MetadataEntity activate(String fqn) {
        log.info("执行草案生效: fqn={}", fqn);

        MetadataEntityDraft draft = draftRepository.findByFqn(fqn)
                .orElseThrow(() -> new DraftNotFoundException("草稿不存在: " + fqn));

        preValidate(draft);

        try {
            MetadataEntity existing = entityRepository.findByFqn(fqn).orElse(null);
            MetadataEntity entity;

            if (existing != null) {
                log.info("覆盖已生效版本: fqn={}, 当前版本={}", fqn, existing.getCurrentVersionValue());
                existing.setName(draft.getName());
                existing.setDescription(draft.getDescription());
                existing.setParentFqn(draft.getParentFqn());
                existing.setContent(draft.getContent());
                existing.incrementVersion();
                existing.setUpdatedBy(draft.getUpdatedBy());
                entity = entityRepository.save(existing);
            } else {
                log.info("首次生效: fqn={}", fqn);
                entity = new MetadataEntity(
                        draft.getFqn(),
                        draft.getName(),
                        draft.getDescription(),
                        draft.getParentFqn(),
                        draft.getEntitySchemaFqn(),
                        draft.getContent(),
                        VersionNumber.initial(),
                        draft.getCreatedBy());
                entity = entityRepository.save(entity);
            }

            EntityVersion version = new EntityVersion(
                    entity.getFqn(),
                    entity.getName(),
                    entity.getDescription(),
                    entity.getParentFqn(),
                    entity.getCurrentVersion(),
                    entity.getEntitySchemaFqn(),
                    entity.getContent(),
                    entity.getUpdatedBy() != null ? entity.getUpdatedBy() : entity.getCreatedBy());
            versionRepository.save(version);
            log.info("历史版本归档成功: fqn={}, version={}", fqn, version.getVersionValue());

            draftRepository.deleteByFqn(fqn);
            log.info("草稿已删除: fqn={}", fqn);

            log.info("草案生效完成: fqn={}, version={}", fqn, entity.getCurrentVersionValue());
            return entity;
        } catch (Exception e) {
            log.error("生效原子事务失败: fqn={}", fqn, e);
            throw new ActivationFailedException("生效原子事务失败: " + fqn, e);
        }
    }

    private void preValidate(MetadataEntityDraft draft) {
        schemaValidationService.validate(draft.getEntitySchemaFqnValue(), draft.getContent());

        String parentFqn = draft.getParentFqn();
        if (parentFqn != null && !parentFqn.isEmpty()) {
            if (!entityRepository.existsByFqn(parentFqn)) {
                throw new ActivationFailedException("父实体未生效: " + parentFqn);
            }
        }

        log.debug("元模型版本有效性校验（上游适配器占位）: {}", draft.getEntitySchemaFqnValue());
    }
}
