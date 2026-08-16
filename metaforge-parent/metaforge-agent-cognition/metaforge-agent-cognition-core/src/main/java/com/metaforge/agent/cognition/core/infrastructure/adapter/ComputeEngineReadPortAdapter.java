package com.metaforge.agent.cognition.core.infrastructure.adapter;

import com.metaforge.agent.cognition.api.port.ComputeEngineReadPort;
import com.metaforge.computeengine.api.service.GraphQueryService;
import com.metaforge.computeengine.api.service.PathReasoningService;
import com.metaforge.computeengine.api.service.ImpactTracingService;
import com.metaforge.computeengine.api.dto.request.*;
import com.metaforge.computeengine.api.enums.AssociationType;
import com.metaforge.common.dto.PageResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ComputeEngineReadPortAdapter implements ComputeEngineReadPort {

    private final GraphQueryService graphQueryService;
    private final PathReasoningService pathReasoningService;
    private final ImpactTracingService impactTracingService;

    public ComputeEngineReadPortAdapter(GraphQueryService graphQueryService,
                                         PathReasoningService pathReasoningService,
                                         ImpactTracingService impactTracingService) {
        this.graphQueryService = graphQueryService;
        this.pathReasoningService = pathReasoningService;
        this.impactTracingService = impactTracingService;
    }

    @Override
    public Object queryAdjacency(Object request) {
        return graphQueryService.queryAdjacency((AdjacencyQueryRequest) request);
    }

    @Override
    public Object queryCompositionTree(Object request) {
        return graphQueryService.queryCompositionTree((CompositionTreeQueryRequest) request);
    }

    @Override
    public Object querySubgraph(Object request) {
        return graphQueryService.querySubgraph((SubgraphQueryRequest) request);
    }

    @Override
    public Object queryPatternMatch(Object request) {
        return graphQueryService.queryPatternMatch((PatternMatchRequest) request);
    }

    @Override
    public PageResult<?> searchCompound(Object request) {
        return graphQueryService.searchCompound((CompoundSearchRequest) request);
    }

    @Override
    public Object queryBatch(Object request) {
        return graphQueryService.queryBatch((BatchQueryRequest) request);
    }

    @Override
    public Object findPaths(Object request) {
        return pathReasoningService.findPaths((PathQueryRequest) request);
    }

    @Override
    public Object computeClosure(Object request) {
        return pathReasoningService.computeClosure((ClosureQueryRequest) request);
    }

    @Override
    public Object multiHopReasoning(Object request) {
        return pathReasoningService.multiHopReasoning((MultiHopQueryRequest) request);
    }

    @Override
    public Object checkReachability(Object request) {
        return pathReasoningService.checkReachability((ReachabilityCheckRequest) request);
    }

    @Override
    public Object diffuseForward(Object request) {
        return impactTracingService.diffuseForward((ImpactDiffusionRequest) request);
    }

    @Override
    public Object traceBackward(Object request) {
        return impactTracingService.traceBackward((ImpactDiffusionRequest) request);
    }

    @Override
    public Object getImpactPaths(String sourceFqn, String targetFqn, List<?> relationTypes, int maxDepth) {
        List<AssociationType> types = relationTypes.stream()
                .filter(AssociationType.class::isInstance)
                .map(AssociationType.class::cast)
                .collect(Collectors.toList());
        return impactTracingService.getImpactPaths(sourceFqn, targetFqn, types, maxDepth);
    }
}
