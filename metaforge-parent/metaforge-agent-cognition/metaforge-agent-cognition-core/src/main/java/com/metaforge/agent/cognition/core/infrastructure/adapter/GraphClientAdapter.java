package com.metaforge.agent.cognition.core.infrastructure.adapter;

import com.metaforge.agent.cognition.core.domain.port.GraphClientPort;
import com.metaforge.graph.api.service.RelationQueryService;
import com.metaforge.graph.api.service.RelationTopologyService;
import com.metaforge.common.dto.PageResult;
import com.metaforge.graph.api.dto.RelationInstanceDto;
import com.metaforge.graph.api.dto.RelationQueryRequest;
import com.metaforge.graph.api.dto.RelationCount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GraphClientAdapter implements GraphClientPort {

    private static final Logger log = LoggerFactory.getLogger(GraphClientAdapter.class);

    private final RelationQueryService relationQueryService;
    private final RelationTopologyService relationTopologyService;

    public GraphClientAdapter(RelationQueryService relationQueryService,
                               RelationTopologyService relationTopologyService) {
        this.relationQueryService = relationQueryService;
        this.relationTopologyService = relationTopologyService;
    }

    @Override
    public Object getOutboundRelations(String entityFqn, List<String> relationTypes, List<String> targetEntityTypes) {
        List<RelationInstanceDto> allRelations = new ArrayList<>();

        String targetFilter = (targetEntityTypes != null && !targetEntityTypes.isEmpty())
                ? targetEntityTypes.get(0) : null;

        if (relationTypes != null && !relationTypes.isEmpty()) {
            for (String relationType : relationTypes) {
                List<RelationInstanceDto> relations = relationQueryService
                        .getOutboundRelations(entityFqn, relationType, targetFilter);
                allRelations.addAll(relations);
            }
        } else {
            List<RelationInstanceDto> relations = relationQueryService
                    .getOutboundRelations(entityFqn, null, targetFilter);
            allRelations.addAll(relations);
        }

        log.debug("查询出边关系: entityFqn={}, count={}", entityFqn, allRelations.size());
        return allRelations;
    }

    @Override
    public Object getInboundRelations(String entityFqn, List<String> relationTypes, List<String> sourceEntityTypes) {
        List<RelationInstanceDto> allRelations = new ArrayList<>();

        String sourceFilter = (sourceEntityTypes != null && !sourceEntityTypes.isEmpty())
                ? sourceEntityTypes.get(0) : null;

        if (relationTypes != null && !relationTypes.isEmpty()) {
            for (String relationType : relationTypes) {
                List<RelationInstanceDto> relations = relationQueryService
                        .getInboundRelations(entityFqn, relationType, sourceFilter);
                allRelations.addAll(relations);
            }
        } else {
            List<RelationInstanceDto> relations = relationQueryService
                    .getInboundRelations(entityFqn, null, sourceFilter);
            allRelations.addAll(relations);
        }

        log.debug("查询入边关系: entityFqn={}, count={}", entityFqn, allRelations.size());
        return allRelations;
    }

    @Override
    public Object multiFilter(Object criteria) {
        if (criteria instanceof RelationQueryRequest request) {
            PageResult<RelationInstanceDto> result = relationQueryService.multiFilter(request);
            log.debug("多条件过滤关系查询: count={}", result != null ? result.getTotal() : 0);
            return result != null ? result.getContent() : List.of();
        }
        log.debug("多条件过滤关系查询: criteria type={}", criteria != null ? criteria.getClass().getName() : "null");
        return List.of();
    }

    @Override
    public Object getDependentRelations(String entityFqn) {
        List<String> relations = relationTopologyService.getDependentRelations(entityFqn);
        log.debug("查询依赖关系: entityFqn={}, count={}", entityFqn, relations.size());
        return relations;
    }

    @Override
    public int getRelationCount(String entityFqn) {
        RelationCount count = relationTopologyService.getRelationCount(entityFqn);
        long total = count.getOutboundCount() + count.getInboundCount();
        log.debug("查询关系数量: entityFqn={}, count={}", entityFqn, total);
        return (int) total;
    }
}
