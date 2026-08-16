package com.metaforge.agent.cognition.operator.procedural;

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

/**
 * 前后步导航——以 relationType=PROCESS_SEQUENCE 统一查询执行单元（ExecutionStep / DecisionStep / Task）
 * 的 1 度流程前后驱，自动覆盖 StepHasNextStep/StepHasNextDecisionStep/StepHasNextTask、
 * DecisionStepHasNextStep/DecisionStepHasNextDecisionStep/DecisionStepHasNextTask、TaskHasNextStep。
 */
@Component
public class ProceduralAdjacentStepOperator extends AbstractCognitionOperator {

    private static final List<String> DEFAULT_PROCESS_SEQUENCE_TYPES = List.of("PROCESS_SEQUENCE");

    @Override
    public String operatorId() {
        return "procedural.adjacent-step";
    }

    @Override
    public DimensionCategory category() {
        return DimensionCategory.PROCEDURAL;
    }

    @Override
    public CognitionResult execute(CognitionQueryContext context) {
        String entityFqn = context.entityFqn();
        if (entityFqn == null || entityFqn.isBlank()) {
            return wrapFailure("缺少 entityFqn 参数");
        }

        List<RelationInstanceDto> next = queryProcessSequence(entityFqn, RelationDirection.OUTBOUND);
        List<RelationInstanceDto> previous = queryProcessSequence(entityFqn, RelationDirection.INBOUND);

        Map<String, Object> resultData = new LinkedHashMap<>();
        resultData.put("current", entityFqn);
        resultData.put("previous", toRelationMaps(previous));
        resultData.put("next", toRelationMaps(next));

        return CognitionResult.success(operatorId(), category(), resultData);
    }

    private List<RelationInstanceDto> queryProcessSequence(String entityFqn, RelationDirection direction) {
        Map<String, Object> localConfig = new LinkedHashMap<>();
        localConfig.put("relationTypes", DEFAULT_PROCESS_SEQUENCE_TYPES);
        localConfig.put("relationSchemaFqns", List.of());
        localConfig.put("relationSchemaFqnPrefix", "");
        Object result = executeWithPort(() -> queryRelationsBySchema(
                entityFqn, direction, localConfig, List.of(), null));
        if (!(result instanceof List<?> list)) {
            return new ArrayList<>();
        }
        List<RelationInstanceDto> out = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof RelationInstanceDto dto) {
                out.add(dto);
            }
        }
        return out;
    }

    private List<Map<String, Object>> toRelationMaps(List<RelationInstanceDto> relations) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (RelationInstanceDto dto : relations) {
            result.add(toRelationSummary(dto));
        }
        return result;
    }
}
