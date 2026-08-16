package com.metaforge.agent.cognition.operator.structural;

import com.metaforge.agent.cognition.api.enums.DimensionCategory;
import com.metaforge.agent.cognition.api.spi.CognitionQueryContext;
import com.metaforge.agent.cognition.api.spi.CognitionResult;
import com.metaforge.agent.cognition.operator.common.AbstractCognitionOperator;
import com.metaforge.graph.api.dto.RelationInstanceDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class StructuralDomainLocatorOperator extends AbstractCognitionOperator {

    @Override
    public String operatorId() {
        return "structural.domain-locator";
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

        List<String> path = resolveDomainLocation(entityFqn);

        Map<String, Object> resultData = new LinkedHashMap<>();
        resultData.put("domain_location", path);
        resultData.put("levels", path.size());
        resultData.put("entityFqn", entityFqn);

        return CognitionResult.success(operatorId(), category(), resultData);
    }

    private List<String> resolveDomainLocation(String entityFqn) {
        List<String> path = new ArrayList<>();
        String current = entityFqn;

        for (int i = 0; i < 10; i++) {
            final String target = current;
            Object result = executeWithPort(() -> graphReadPort.getInboundRelations(target, "COMPOSITION", null));
            if (result instanceof CognitionResult) break;

            if (result instanceof List<?> list && !list.isEmpty()) {
                Object first = list.get(0);
                if (!(first instanceof RelationInstanceDto edge)) break;
                String parent = edge.getSourceEntityFqn();
                if (parent != null && !parent.equals(current)) {
                    path.add(0, parent);
                    current = parent;
                } else {
                    break;
                }
            } else {
                break;
            }
        }

        path.add(entityFqn);
        return path;
    }
}
