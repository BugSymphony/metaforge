package com.metaforge.agent.cognition.core.domain.port;

import java.util.List;

public interface ComputeEngineClientPort {

    Object queryAdjacency(String sourceFqn, String direction, int maxDepth, List<String> relationTypes);

    Object queryCompositionTree(String rootFqn, String direction, int maxDepth);

    Object querySubgraph(List<String> centerFqns, int expandDepth, List<String> relationTypes);

    Object diffuseForward(String sourceFqn, List<String> relationTypes, int maxDepth);

    Object traceBackward(String sourceFqn, List<String> relationTypes, int maxDepth);

    Object getImpactPaths(String sourceFqn, String targetFqn, List<String> relationTypes, int maxDepth);

    Object computeClosure(String sourceFqn, List<String> relationTypes);

    Object queryBatch(List<String> fqns, int page, int size);

    Object searchCompound(List<String> entityTypes, List<Object> conditions, int page, int size);
}
