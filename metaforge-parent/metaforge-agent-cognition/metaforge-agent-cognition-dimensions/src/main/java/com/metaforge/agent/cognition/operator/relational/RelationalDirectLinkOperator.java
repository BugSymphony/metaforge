package com.metaforge.agent.cognition.operator.relational;

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
public class RelationalDirectLinkOperator extends AbstractCognitionOperator {

    @Override
    public String operatorId() {
        return "relational.direct-link";
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

        final String fqn = entityFqn;
        Object outboundResult = executeWithPort(() -> graphReadPort.getOutboundRelations(fqn, null, null));
        if (outboundResult instanceof CognitionResult cr) return cr;

        Object inboundResult = executeWithPort(() -> graphReadPort.getInboundRelations(fqn, null, null));
        if (inboundResult instanceof CognitionResult cr) return cr;

        List<Map<String, Object>> outbound = toRelationMaps(outboundResult);
        List<Map<String, Object>> inbound = toRelationMaps(inboundResult);

        Map<String, Object> resultData = new LinkedHashMap<>();
        resultData.put("outbound", outbound);
        resultData.put("inbound", inbound);
        resultData.put("entityFqn", entityFqn);

        return CognitionResult.success(operatorId(), category(), resultData);
    }

    private List<Map<String, Object>> toRelationMaps(Object portResult) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (portResult instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof RelationInstanceDto dto) {
                    result.add(toRelationSummary(dto));
                }
            }
        }
        return result;
    }
}
