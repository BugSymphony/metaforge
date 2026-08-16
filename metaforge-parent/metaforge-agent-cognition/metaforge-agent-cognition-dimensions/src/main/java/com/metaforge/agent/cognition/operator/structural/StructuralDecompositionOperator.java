package com.metaforge.agent.cognition.operator.structural;

import com.metaforge.agent.cognition.api.enums.DimensionCategory;
import com.metaforge.agent.cognition.api.spi.CognitionQueryContext;
import com.metaforge.agent.cognition.api.spi.CognitionResult;
import com.metaforge.agent.cognition.operator.common.AbstractCognitionOperator;
import com.metaforge.computeengine.api.dto.request.CompositionTreeQueryRequest;
import com.metaforge.computeengine.api.enums.TraversalDirection;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class StructuralDecompositionOperator extends AbstractCognitionOperator {

    @Override
    public String operatorId() {
        return "structural.decomposition";
    }

    @Override
    public DimensionCategory category() {
        return DimensionCategory.STRUCTURAL;
    }

    @Override
    public CognitionResult execute(CognitionQueryContext context) {
        String entityFqn = context.entityFqn();
        if (entityFqn == null || entityFqn.isBlank()) {
            return wrapFailure("缺少 entityFqn 参数");
        }

        CompositionTreeQueryRequest request = new CompositionTreeQueryRequest(
                entityFqn, TraversalDirection.FORWARD, 5, null);

        Object portResult = executeWithPort(() -> computeEngineReadPort.queryCompositionTree(request));
        if (portResult instanceof CognitionResult cr) {
            return cr;
        }

        Map<String, Object> tree = new LinkedHashMap<>();
        if (portResult instanceof Map<?, ?> m) {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) m;
            tree.putAll(result);
        }

        Object children = tree.get("children");
        boolean hasChildren = children instanceof List<?> list && !list.isEmpty();

        Map<String, Object> resultData = new LinkedHashMap<>();
        resultData.put("root", tree.get("root"));
        resultData.put("children", children);
        resultData.put("has_children", hasChildren);

        return CognitionResult.success(operatorId(), category(), resultData);
    }
}
