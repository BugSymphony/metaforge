package com.metaforge.agent.cognition.core.infrastructure.adapter;

import com.metaforge.agent.cognition.api.port.MetadataReadPort;
import com.metaforge.common.dto.PageRequest;
import com.metaforge.common.dto.PageResult;
import com.metaforge.metadata.api.dto.request.MetadataQueryRequest;
import com.metaforge.metadata.api.service.MetadataQueryService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MetadataReadPortAdapter implements MetadataReadPort {

    private final MetadataQueryService metadataQueryService;

    public MetadataReadPortAdapter(MetadataQueryService metadataQueryService) {
        this.metadataQueryService = metadataQueryService;
    }

    @Override
    public Object getByFqn(String fqn) {
        return metadataQueryService.getByFqn(fqn);
    }

    @Override
    public PageResult<?> listByFqnPrefixes(List<String> fqnPrefixes, PageRequest pageRequest) {
        MetadataQueryRequest request = new MetadataQueryRequest();
        request.setFqnPrefixes(fqnPrefixes);
        request.setPageRequest(pageRequest);
        return metadataQueryService.listByFqnPrefixes(request);
    }

    @Override
    public PageResult<?> listByEntitySchema(String entitySchemaFqn, PageRequest pageRequest) {
        MetadataQueryRequest request = new MetadataQueryRequest();
        request.setEntitySchemaFqn(entitySchemaFqn);
        request.setPageRequest(pageRequest);
        return metadataQueryService.listByEntitySchema(request);
    }
}
