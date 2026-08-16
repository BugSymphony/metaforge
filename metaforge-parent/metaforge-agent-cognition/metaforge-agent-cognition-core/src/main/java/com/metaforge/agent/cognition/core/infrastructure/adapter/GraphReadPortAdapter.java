package com.metaforge.agent.cognition.core.infrastructure.adapter;

import com.metaforge.agent.cognition.api.port.GraphReadPort;
import com.metaforge.common.dto.PageRequest;
import com.metaforge.common.dto.PageResult;
import com.metaforge.graph.api.service.RelationQueryService;
import com.metaforge.graph.api.service.RelationTopologyService;
import com.metaforge.graph.api.dto.RelationQueryRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GraphReadPortAdapter implements GraphReadPort {

    private final RelationQueryService relationQueryService;
    private final RelationTopologyService relationTopologyService;

    public GraphReadPortAdapter(RelationQueryService relationQueryService,
                                 RelationTopologyService relationTopologyService) {
        this.relationQueryService = relationQueryService;
        this.relationTopologyService = relationTopologyService;
    }

    @Override
    public Object getByFqn(String fqn) {
        return relationQueryService.getByFqn(fqn);
    }

    @Override
    public List<?> getOutboundRelations(String entityFqn, String relationType, String targetEntityType) {
        return relationQueryService.getOutboundRelations(entityFqn, relationType, targetEntityType);
    }

    @Override
    public List<?> getInboundRelations(String entityFqn, String relationType, String sourceEntityType) {
        return relationQueryService.getInboundRelations(entityFqn, relationType, sourceEntityType);
    }

    @Override
    public PageResult<?> multiFilter(Object request) {
        return relationQueryService.multiFilter((RelationQueryRequest) request);
    }

    @Override
    public Object getRelationCount(String entityFqn) {
        return relationTopologyService.getRelationCount(entityFqn);
    }

    @Override
    public PageResult<?> listByConditions(String fqnPrefix, String relationSchemaFqn, PageRequest pageRequest) {
        return relationQueryService.listByConditions(fqnPrefix, relationSchemaFqn, pageRequest);
    }
}
