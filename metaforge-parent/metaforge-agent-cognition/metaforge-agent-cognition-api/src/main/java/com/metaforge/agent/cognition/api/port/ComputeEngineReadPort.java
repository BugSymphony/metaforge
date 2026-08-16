package com.metaforge.agent.cognition.api.port;

import com.metaforge.common.dto.PageResult;

import java.util.List;

/**
 * 语义查询引擎 BC 只读端口，全量查询与推理能力。
 * 上游 Provider: semantic-query-engine (GraphQueryService, PathReasoningService, ImpactTracingService)
 */
public interface ComputeEngineReadPort {

    Object queryAdjacency(Object request);

    Object queryCompositionTree(Object request);

    Object querySubgraph(Object request);

    Object queryPatternMatch(Object request);

    PageResult<?> searchCompound(Object request);

    Object queryBatch(Object request);

    Object findPaths(Object request);

    Object computeClosure(Object request);

    Object multiHopReasoning(Object request);

    Object checkReachability(Object request);

    Object diffuseForward(Object request);

    Object traceBackward(Object request);

    Object getImpactPaths(String sourceFqn, String targetFqn, List<?> relationTypes, int maxDepth);
}
