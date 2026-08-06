package com.metaforge.agent.cognition.core.infrastructure.adapter;

import com.metaforge.agent.cognition.core.domain.port.MetadataClientPort;
import com.metaforge.metadata.api.service.MetadataQueryService;
import com.metaforge.common.dto.PageRequest;
import com.metaforge.common.dto.PageResult;
import com.metaforge.metadata.api.dto.request.MetadataQueryRequest;
import com.metaforge.metadata.api.dto.response.MetadataEntityDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MetadataClientAdapter implements MetadataClientPort {

    private static final Logger log = LoggerFactory.getLogger(MetadataClientAdapter.class);

    private final MetadataQueryService metadataQueryService;

    public MetadataClientAdapter(MetadataQueryService metadataQueryService) {
        this.metadataQueryService = metadataQueryService;
    }

    @Override
    public Object getByFqn(String fqn) {
        MetadataEntityDto entity = metadataQueryService.getByFqn(fqn);
        log.debug("按 FQN 查询元数据: fqn={}, found={}", fqn, entity != null);
        return entity;
    }

    @Override
    public Object listByFqnPrefixes(List<String> fqnPrefixes, int page, int size) {
        PageRequest pageRequest = buildPageRequest(page, size);
        MetadataQueryRequest request = new MetadataQueryRequest();
        request.setFqnPrefixes(fqnPrefixes);
        request.setPageRequest(pageRequest);

        PageResult<MetadataEntityDto> result = metadataQueryService.listByFqnPrefixes(request);
        List<MetadataEntityDto> entities = result != null ? result.getContent() : List.of();
        log.debug("按前缀查询元数据列表: prefixes={}, count={}", fqnPrefixes, entities.size());
        return entities;
    }

    @Override
    public Object listByEntitySchema(String entitySchemaFqn, int page, int size) {
        PageRequest pageRequest = buildPageRequest(page, size);
        MetadataQueryRequest request = new MetadataQueryRequest();
        request.setEntitySchemaFqn(entitySchemaFqn);
        request.setPageRequest(pageRequest);

        PageResult<MetadataEntityDto> result = metadataQueryService.listByEntitySchema(request);
        List<MetadataEntityDto> entities = result != null ? result.getContent() : List.of();
        log.debug("按 EntitySchema 查询元数据: schema={}, count={}", entitySchemaFqn, entities.size());
        return entities;
    }

    @Override
    public Object queryByAttributes(List<Object> conditions, String matchMode) {
        log.debug("按属性条件查询元数据: mode={}, conditionsCount={}",
                matchMode, conditions != null ? conditions.size() : 0);
        return List.of();
    }

    private PageRequest buildPageRequest(int page, int size) {
        PageRequest pageRequest = new PageRequest();
        pageRequest.setPage(page);
        pageRequest.setSize(size);
        return pageRequest;
    }
}
