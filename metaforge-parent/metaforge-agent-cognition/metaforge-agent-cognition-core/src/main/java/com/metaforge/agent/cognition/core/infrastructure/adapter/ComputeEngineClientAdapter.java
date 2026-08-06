package com.metaforge.agent.cognition.core.infrastructure.adapter;

import com.metaforge.agent.cognition.core.domain.port.ComputeEngineClientPort;
import com.metaforge.computeengine.api.service.GraphQueryService;
import com.metaforge.computeengine.api.service.ImpactTracingService;
import com.metaforge.computeengine.api.service.PathReasoningService;
import com.metaforge.computeengine.api.dto.request.*;
import com.metaforge.computeengine.api.dto.response.GraphQueryResult;
import com.metaforge.computeengine.api.dto.response.ImpactTraceResult;
import com.metaforge.computeengine.api.enums.AssociationType;
import com.metaforge.computeengine.api.enums.TraversalDirection;
import com.metaforge.common.dto.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class ComputeEngineClientAdapter implements ComputeEngineClientPort {

    private static final Logger log = LoggerFactory.getLogger(ComputeEngineClientAdapter.class);

    private final GraphQueryService graphQueryService;
    private final ImpactTracingService impactTracingService;
    private final PathReasoningService pathReasoningService;

    public ComputeEngineClientAdapter(GraphQueryService graphQueryService,
                                       ImpactTracingService impactTracingService,
                                       PathReasoningService pathReasoningService) {
        this.graphQueryService = graphQueryService;
        this.impactTracingService = impactTracingService;
        this.pathReasoningService = pathReasoningService;
    }

    @Override
    public Object queryAdjacency(String sourceFqn, String direction, int maxDepth, List<String> relationTypes) {
        TraversalDirection dir = resolveDirection(direction);
        Set<AssociationType> types = resolveAssociationTypes(relationTypes);

        AdjacencyQueryRequest request = new AdjacencyQueryRequest(sourceFqn, dir, maxDepth, types, null);
        GraphQueryResult result = graphQueryService.queryAdjacency(request);
        log.debug("邻接查询: source={}, direction={}, depth={}, entities={}",
                sourceFqn, direction, maxDepth,
                result != null && result.entities() != null ? result.entities().size() : 0);
        return result;
    }

    @Override
    public Object queryCompositionTree(String rootFqn, String direction, int maxDepth) {
        TraversalDirection dir = resolveDirection(direction);

        CompositionTreeQueryRequest request = new CompositionTreeQueryRequest(rootFqn, dir, maxDepth, null);
        GraphQueryResult result = graphQueryService.queryCompositionTree(request);
        log.debug("组成树查询: root={}, direction={}, depth={}, entities={}",
                rootFqn, direction, maxDepth,
                result != null && result.entities() != null ? result.entities().size() : 0);
        return result;
    }

    @Override
    public Object querySubgraph(List<String> centerFqns, int expandDepth, List<String> relationTypes) {
        SubgraphQueryRequest request = new SubgraphQueryRequest(centerFqns, expandDepth, null);
        GraphQueryResult result = graphQueryService.querySubgraph(request);
        log.debug("子图查询: centers={}, depth={}, entities={}",
                centerFqns != null ? centerFqns.size() : 0, expandDepth,
                result != null && result.entities() != null ? result.entities().size() : 0);
        return result;
    }

    @Override
    public Object diffuseForward(String sourceFqn, List<String> relationTypes, int maxDepth) {
        Set<AssociationType> types = resolveAssociationTypes(relationTypes);

        ImpactDiffusionRequest request = new ImpactDiffusionRequest(
                sourceFqn, TraversalDirection.FORWARD, maxDepth, types);
        ImpactTraceResult result = impactTracingService.diffuseForward(request);
        log.debug("正向扩散: source={}, depth={}", sourceFqn, maxDepth);
        return result;
    }

    @Override
    public Object traceBackward(String sourceFqn, List<String> relationTypes, int maxDepth) {
        Set<AssociationType> types = resolveAssociationTypes(relationTypes);

        ImpactDiffusionRequest request = new ImpactDiffusionRequest(
                sourceFqn, TraversalDirection.BACKWARD, maxDepth, types);
        ImpactTraceResult result = impactTracingService.traceBackward(request);
        log.debug("反向追溯: source={}, depth={}", sourceFqn, maxDepth);
        return result;
    }

    @Override
    public Object getImpactPaths(String sourceFqn, String targetFqn, List<String> relationTypes, int maxDepth) {
        Set<AssociationType> types = resolveAssociationTypes(relationTypes);

        ImpactTraceResult result = impactTracingService.getImpactPaths(
                sourceFqn, targetFqn, new ArrayList<>(types), maxDepth);
        log.debug("影响路径查询: source={}, target={}", sourceFqn, targetFqn);
        return result;
    }

    @Override
    public Object computeClosure(String sourceFqn, List<String> relationTypes) {
        Set<AssociationType> types = resolveAssociationTypes(relationTypes);

        ClosureQueryRequest request = new ClosureQueryRequest(sourceFqn, types, null);
        var result = pathReasoningService.computeClosure(request);
        log.debug("闭包计算: source={}", sourceFqn);
        return result;
    }

    @Override
    public Object queryBatch(List<String> fqns, int page, int size) {
        BatchQueryRequest request = new BatchQueryRequest(fqns);
        GraphQueryResult result = graphQueryService.queryBatch(request);
        log.debug("批量查询: count={}", fqns != null ? fqns.size() : 0);
        return result;
    }

    @Override
    public Object searchCompound(List<String> entityTypes, List<Object> conditions, int page, int size) {
        CompoundSearchRequest request = new CompoundSearchRequest(
                entityTypes, null, null, page, size, null, null);
        PageResult<?> result = graphQueryService.searchCompound(request);
        log.debug("复合搜索: types={}", entityTypes);
        return result;
    }

    private TraversalDirection resolveDirection(String direction) {
        if (direction == null) return TraversalDirection.FORWARD;
        return switch (direction.toUpperCase()) {
            case "BACKWARD" -> TraversalDirection.BACKWARD;
            case "BOTH", "BIDIRECTIONAL" -> TraversalDirection.BIDIRECTIONAL;
            default -> TraversalDirection.FORWARD;
        };
    }

    private Set<AssociationType> resolveAssociationTypes(List<String> relationTypes) {
        if (relationTypes == null || relationTypes.isEmpty()) return Set.of();
        Set<AssociationType> types = new HashSet<>();
        for (String type : relationTypes) {
            try {
                types.add(AssociationType.valueOf(type));
            } catch (IllegalArgumentException e) {
                log.warn("未知关联类型: {}", type);
            }
        }
        return types;
    }
}
