package com.metaforge.agent.cognition.operator.relational;

import com.metaforge.agent.cognition.api.enums.DimensionCategory;
import com.metaforge.agent.cognition.api.spi.CognitionQueryContext;
import com.metaforge.agent.cognition.api.spi.CognitionResult;
import com.metaforge.agent.cognition.operator.common.AbstractCognitionOperator;
import com.metaforge.computeengine.api.dto.request.AdjacencyQueryRequest;
import com.metaforge.computeengine.api.dto.response.GraphQueryResult;
import com.metaforge.computeengine.api.enums.TraversalDirection;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class RelationalNeighborhoodOperator extends AbstractCognitionOperator {

    @Override
    public String operatorId() {
        return "relational.neighborhood";
    }

    @Override
    public DimensionCategory category() {
        return DimensionCategory.RELATIONAL;
    }

    @Override
    public CognitionResult execute(CognitionQueryContext context) {
        String entityFqn = context.entityFqn();
        if (entityFqn == null || entityFqn.isBlank()) {
            return wrapFailure("缺少 entityFqn 参数");
        }

        int maxDepth = resolveMaxDepth(context);

        AdjacencyQueryRequest request = new AdjacencyQueryRequest(
                entityFqn, TraversalDirection.BIDIRECTIONAL, maxDepth, Set.of(), null);

        Object portResult = executeWithPort(() -> computeEngineReadPort.queryAdjacency(request));
        if (portResult instanceof CognitionResult cr) {
            return cr;
        }

        Map<String, Object> resultData = new LinkedHashMap<>();
        resultData.put("maxDepth", maxDepth);
        resultData.put("entityFqn", entityFqn);
        if (portResult instanceof GraphQueryResult result) {
            resultData.put("entities", result.entities() != null ? result.entities() : List.of());
            resultData.put("relations", result.relations() != null ? result.relations() : List.of());
            resultData.put("adjacency_map", result.adjacencyMap() != null ? result.adjacencyMap() : Map.of());
            resultData.put("truncated", result.truncated());
        }

        return CognitionResult.success(operatorId(), category(), resultData);
    }

    private int resolveMaxDepth(CognitionQueryContext context) {
        Map<String, Object> params = context.templateParams();
        if (params != null) {
            for (String key : new String[]{"max_depth", "maxDepth"}) {
                if (params.containsKey(key)) {
                    try {
                        int depth = Integer.parseInt(params.get(key).toString());
                        return Math.min(depth, 3);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return 2;
    }
}
