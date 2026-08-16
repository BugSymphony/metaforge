package com.metaforge.agent.cognition.operator.structural;

import com.metaforge.agent.cognition.api.enums.DimensionCategory;
import com.metaforge.agent.cognition.api.spi.CognitionQueryContext;
import com.metaforge.agent.cognition.api.spi.CognitionResult;
import com.metaforge.agent.cognition.operator.common.AbstractCognitionOperator;
import com.metaforge.computeengine.api.dto.request.CompositionTreeQueryRequest;
import com.metaforge.computeengine.api.enums.TraversalDirection;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class StructuralBelongingOperator extends AbstractCognitionOperator {

    @Override
    public String operatorId() {
        return "structural.belonging";
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
                entityFqn, TraversalDirection.BACKWARD, 5, null);

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

        return CognitionResult.success(operatorId(), category(), tree);
    }
}
