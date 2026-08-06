package com.metaforge.metadata.application.service;

import com.metaforge.metadata.api.dto.response.DeactivationCheckResult;
import com.metaforge.metadata.api.dto.response.MetadataEntityDto;
import com.metaforge.metadata.api.enums.ChangeType;
import com.metaforge.metadata.api.event.MetadataChangeEvent;
import com.metaforge.metadata.api.service.MetadataActivationService;
import com.metaforge.metadata.domain.event.MetadataEventPublisher;
import com.metaforge.metadata.domain.exception.EntityNotFoundException;
import com.metaforge.metadata.domain.model.aggregate.MetadataEntity;
import com.metaforge.metadata.domain.model.entity.EntityVersion;
import com.metaforge.metadata.domain.repository.EntityVersionRepository;
import com.metaforge.metadata.domain.repository.MetadataEntityRepository;
import com.metaforge.metadata.domain.service.DraftActivationService;
import com.metaforge.metadata.domain.service.EntityDeactivationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class MetadataActivationServiceImpl implements MetadataActivationService {

    private static final Logger log = LoggerFactory.getLogger(MetadataActivationServiceImpl.class);

    private final DraftActivationService draftActivationService;
    private final EntityDeactivationService entityDeactivationService;
    private final MetadataEntityRepository entityRepository;
    private final EntityVersionRepository versionRepository;
    private final MetadataEventPublisher eventPublisher;

    public MetadataActivationServiceImpl(DraftActivationService draftActivationService,
                                         EntityDeactivationService entityDeactivationService,
                                         MetadataEntityRepository entityRepository,
                                         EntityVersionRepository versionRepository,
                                         MetadataEventPublisher eventPublisher) {
        this.draftActivationService = draftActivationService;
        this.entityDeactivationService = entityDeactivationService;
        this.entityRepository = entityRepository;
        this.versionRepository = versionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public MetadataEntityDto activate(String fqn) {
        MetadataEntity entity = draftActivationService.activate(fqn);
        eventPublisher.publish(
                new MetadataChangeEvent(this, fqn, ChangeType.ACTIVATE, entity.getCurrentVersionValue()));
        return toEntityDto(entity);
    }

    @Override
    public void deactivate(String fqn) {
        entityDeactivationService.deactivate(fqn);
        eventPublisher.publish(
                new MetadataChangeEvent(this, fqn, ChangeType.DEPRECATE, null));
    }

    @Override
    public MetadataEntityDto reactivate(String fqn) {
        log.info("从历史版本重新生效: fqn={}", fqn);

        List<EntityVersion> versions = versionRepository.findByFqnOrderByVersionDesc(fqn);
        if (versions.isEmpty()) {
            throw new EntityNotFoundException("历史归档记录不存在: " + fqn);
        }

        if (entityRepository.existsByFqn(fqn)) {
            log.info("FQN 已处于生效态，直接返回: {}", fqn);
            return entityRepository.findByFqn(fqn)
                    .map(this::toEntityDto)
                    .orElseThrow(() -> new EntityNotFoundException("生效元数据不存在: " + fqn));
        }

        EntityVersion latestVersion = versions.get(0);

        MetadataEntity entity = new MetadataEntity(
                latestVersion.getFqn(),
                latestVersion.getName(),
                latestVersion.getDescription(),
                latestVersion.getParentFqn(),
                latestVersion.getEntitySchemaFqn(),
                latestVersion.getContent(),
                latestVersion.getVersion(),
                latestVersion.getCreatedBy());
        entity.setCreatedTime(latestVersion.getCreatedTime());

        MetadataEntity saved = entityRepository.save(entity);
        log.info("从历史版本重新生效完成: fqn={}, version={}", fqn, saved.getCurrentVersionValue());

        eventPublisher.publish(
                new MetadataChangeEvent(this, fqn, ChangeType.ACTIVATE, saved.getCurrentVersionValue()));
        return toEntityDto(saved);
    }

    @Override
    public DeactivationCheckResult checkDeactivationPreconditions(String fqn) {
        if (!entityRepository.existsByFqn(fqn)) {
            throw new EntityNotFoundException("生效元数据不存在: " + fqn);
        }
        return entityDeactivationService.checkPreconditions(fqn);
    }

    private MetadataEntityDto toEntityDto(MetadataEntity entity) {
        MetadataEntityDto dto = new MetadataEntityDto();
        dto.setId(entity.getId());
        dto.setFqn(entity.getFqnValue());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setParentFqn(entity.getParentFqn());
        dto.setEntitySchemaFqn(entity.getEntitySchemaFqnValue());
        dto.setContent(entity.getContent());
        dto.setCurrentVersion(entity.getCurrentVersionValue());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedTime(entity.getCreatedTime());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setUpdatedTime(entity.getUpdatedTime());
        return dto;
    }
}
