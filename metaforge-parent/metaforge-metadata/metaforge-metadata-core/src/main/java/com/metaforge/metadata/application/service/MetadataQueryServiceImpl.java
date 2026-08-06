package com.metaforge.metadata.application.service;

import com.metaforge.common.dto.PageRequest;
import com.metaforge.common.dto.PageResult;
import com.metaforge.common.util.JsonbUtils;
import com.metaforge.metadata.api.dto.request.AdminQueryRequest;
import com.metaforge.metadata.api.dto.request.AttributeCondition;
import com.metaforge.metadata.api.dto.request.MetadataQueryRequest;
import com.metaforge.metadata.api.constants.MetadataStatusConstants;
import com.metaforge.metadata.api.dto.response.MetadataEntityDto;
import com.metaforge.metadata.api.enums.MetadataStatus;
import com.metaforge.metadata.api.service.MetadataQueryService;
import com.metaforge.metadata.domain.exception.EntityNotFoundException;
import com.metaforge.metadata.domain.model.aggregate.MetadataEntity;
import com.metaforge.metadata.domain.repository.MetadataEntityRepository;
import com.metaforge.metadata.infrastructure.mapper.MetadataEntityMapper;
import com.metaforge.metadata.infrastructure.persistence.adapter.MetadataEntityRepositoryImpl;
import com.metaforge.metadata.infrastructure.persistence.jpa.EntityVersionJpaRepository;
import com.metaforge.metadata.infrastructure.persistence.jpa.EntityVersionJpo;
import com.metaforge.metadata.infrastructure.persistence.jpa.MetadataEntityDraftJpaRepository;
import com.metaforge.metadata.infrastructure.persistence.jpa.MetadataEntityDraftJpo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class MetadataQueryServiceImpl implements MetadataQueryService {

    private static final Logger log = LoggerFactory.getLogger(MetadataQueryServiceImpl.class);

    private final MetadataEntityRepository entityRepository;
    private final MetadataEntityRepositoryImpl entityRepositoryImpl;
    private final MetadataEntityMapper entityMapper;
    private final MetadataEntityDraftJpaRepository draftJpaRepository;
    private final EntityVersionJpaRepository versionJpaRepository;

    public MetadataQueryServiceImpl(MetadataEntityRepository entityRepository,
                                    MetadataEntityRepositoryImpl entityRepositoryImpl,
                                    MetadataEntityMapper entityMapper,
                                    MetadataEntityDraftJpaRepository draftJpaRepository,
                                    EntityVersionJpaRepository versionJpaRepository) {
        this.entityRepository = entityRepository;
        this.entityRepositoryImpl = entityRepositoryImpl;
        this.entityMapper = entityMapper;
        this.draftJpaRepository = draftJpaRepository;
        this.versionJpaRepository = versionJpaRepository;
    }

    @Override
    public MetadataEntityDto getByFqn(String fqn) {
        MetadataEntity entity = entityRepository.findByFqn(fqn)
                .orElseThrow(() -> new EntityNotFoundException("元数据实体不存在或已下线: " + fqn));
        return toEntityDto(entity);
    }

    @Override
    public PageResult<MetadataEntityDto> listByFqnPrefixes(MetadataQueryRequest request) {
        List<String> prefixes = request.getFqnPrefixes();
        PageRequest pageRequest = request.getPageRequest();

        if (prefixes == null || prefixes.isEmpty()) {
            return emptyPage(pageRequest);
        }

        PageResult<MetadataEntity> page = entityRepositoryImpl
                .findByFqnPrefixIn(prefixes, pageRequest);

        return mapPage(page);
    }

    @Override
    public PageResult<MetadataEntityDto> listByEntitySchema(MetadataQueryRequest request) {
        String entitySchemaFqn = request.getEntitySchemaFqn();
        PageRequest pageRequest = request.getPageRequest();

        if (entitySchemaFqn == null || entitySchemaFqn.isEmpty()) {
            return emptyPage(pageRequest);
        }

        PageResult<MetadataEntity> page = entityRepositoryImpl
                .findByEntitySchemaFqn(entitySchemaFqn, pageRequest);

        return mapPage(page);
    }

    @Override
    public PageResult<MetadataEntityDto> queryByAttributes(List<AttributeCondition> conditions,
                                                            PageRequest pageRequest) {
        if (conditions == null || conditions.isEmpty()) {
            return emptyPage(pageRequest);
        }

        Map<String, Object> conditionMap = new HashMap<>();
        for (AttributeCondition condition : conditions) {
            if (condition.getField() != null && condition.getValue() != null) {
                conditionMap.put(condition.getField(), condition.getValue());
            }
        }

        String conditionJson = JsonbUtils.toJsonb(conditionMap);

        PageResult<MetadataEntity> page;
        if (pageRequest != null) {
            page = entityRepositoryImpl.findByContentExactMatch(conditionJson, pageRequest);
        } else {
            List<MetadataEntity> results = entityRepositoryImpl.findByContentExactMatch(conditionJson);
            page = new PageResult<>(results, results.size(), 1, results.size());
        }

        return mapPage(page);
    }

    @Override
    public PageResult<MetadataEntityDto> adminQuery(AdminQueryRequest request) {
        PageRequest pageRequest = request.getPageRequest();
        int pageNum = pageRequest != null ? pageRequest.getPage() : 1;
        int pageSize = pageRequest != null ? pageRequest.getSize() : 20;

        List<MetadataEntityDto> allDtos = new ArrayList<>();

        // 主表（ACTIVE）
        for (MetadataEntity e : entityRepositoryImpl.findAll()) {
            MetadataEntityDto dto = toEntityDto(e);
            dto.setStatus(MetadataStatus.ACTIVE.name());
            allDtos.add(dto);
        }

        // 草稿表（DRAFT）
        List<MetadataEntityDraftJpo> draftJpos = draftJpaRepository.findAll();
        for (MetadataEntityDraftJpo jpo : draftJpos) {
            MetadataEntityDto dto = new MetadataEntityDto();
            dto.setFqn(jpo.getFqn());
            dto.setName(jpo.getName());
            dto.setDescription(jpo.getDescription());
            dto.setParentFqn(jpo.getParentFqn());
            dto.setEntitySchemaFqn(jpo.getEntitySchemaFqn());
            dto.setContent(parseContent(jpo.getContent()));
            dto.setCreatedBy(jpo.getCreatedBy());
            dto.setCreatedTime(jpo.getCreatedTime());
            dto.setUpdatedBy(jpo.getUpdatedBy());
            dto.setUpdatedTime(jpo.getUpdatedTime());
            dto.setStatus(MetadataStatus.DRAFT.name());
            allDtos.add(dto);
        }

        // 历史表（HISTORY）
        List<EntityVersionJpo> versionJpos = versionJpaRepository.findAll();
        for (EntityVersionJpo jpo : versionJpos) {
            MetadataEntityDto dto = new MetadataEntityDto();
            dto.setFqn(jpo.getFqn());
            dto.setName(jpo.getName());
            dto.setDescription(jpo.getDescription());
            dto.setParentFqn(jpo.getParentFqn());
            dto.setEntitySchemaFqn(jpo.getEntitySchemaFqn());
            dto.setContent(parseContent(jpo.getContent()));
            dto.setCurrentVersion(jpo.getVersion());
            dto.setCreatedBy(jpo.getCreatedBy());
            dto.setCreatedTime(jpo.getCreatedTime());
            dto.setStatus(MetadataStatusConstants.HISTORY);
            allDtos.add(dto);
        }

        // fqn 过滤
        if (request.getFqn() != null && !request.getFqn().isEmpty()) {
            allDtos.removeIf(dto -> !request.getFqn().equals(dto.getFqn()));
        }

        // statuses 过滤（DRAFT / ACTIVE / HISTORY，大小写不敏感）
        List<String> statuses = request.getStatuses();
        if (statuses != null && !statuses.isEmpty()) {
            List<String> normalized = statuses.stream()
                    .filter(s -> s != null)
                    .map(String::toUpperCase)
                    .toList();
            allDtos.removeIf(dto -> dto.getStatus() == null
                    || !normalized.contains(dto.getStatus().toUpperCase()));
        }

        // 手动分页
        int total = allDtos.size();
        int fromIndex = Math.min((pageNum - 1) * pageSize, total);
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<MetadataEntityDto> pageContent = fromIndex < toIndex
                ? allDtos.subList(fromIndex, toIndex)
                : Collections.emptyList();

        return new PageResult<>(pageContent, total, pageNum, pageSize);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseContent(String contentJson) {
        if (contentJson == null || contentJson.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return JsonbUtils.fromJsonb(contentJson, Map.class);
        } catch (Exception e) {
            log.warn("JSONB 内容解析失败", e);
            return Collections.emptyMap();
        }
    }

    private PageResult<MetadataEntityDto> mapPage(PageResult<MetadataEntity> page) {
        List<MetadataEntityDto> dtos = page.getContent().stream()
                .map(this::toEntityDto)
                .collect(Collectors.toList());
        return new PageResult<>(dtos, page.getTotal(), page.getPage(), page.getSize());
    }

    private PageResult<MetadataEntityDto> emptyPage(PageRequest pageRequest) {
        int page = pageRequest != null ? pageRequest.getPage() : 1;
        int size = pageRequest != null ? pageRequest.getSize() : 20;
        return new PageResult<>(Collections.emptyList(), 0, page, size);
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
