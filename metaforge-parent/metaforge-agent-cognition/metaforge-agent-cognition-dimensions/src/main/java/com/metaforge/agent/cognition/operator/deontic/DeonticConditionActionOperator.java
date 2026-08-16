package com.metaforge.agent.cognition.operator.deontic;

import com.metaforge.agent.cognition.api.enums.DimensionCategory;
import com.metaforge.agent.cognition.api.spi.CognitionQueryContext;
import com.metaforge.agent.cognition.api.spi.CognitionResult;
import com.metaforge.agent.cognition.operator.common.AbstractCognitionOperator;
import com.metaforge.metadata.api.dto.response.MetadataEntityDto;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DeonticConditionActionOperator extends AbstractCognitionOperator {

    @Override
    public String operatorId() {
        return "deontic.condition-action";
    }

    @Override
    public DimensionCategory category() {
        return DimensionCategory.DEONTIC;
    }

    @Override
    public CognitionResult execute(CognitionQueryContext context) {
        String entityFqn = context.entityFqn();
        if (entityFqn == null || entityFqn.isBlank()) {
            return wrapFailure("缺少 entityFqn 参数");
        }

        String conditionField = configString(context.operatorConfig(), "conditionField", "condition");
        String actionField = configString(context.operatorConfig(), "actionField", "action");

        Object portResult = executeWithPort(() -> metadataReadPort.getByFqn(entityFqn));
        if (portResult instanceof CognitionResult cr) return cr;

        String condition = "";
        String action = "";
        if (portResult instanceof MetadataEntityDto dto) {
            Object conditionValue = resolveContentValue(dto, conditionField);
            Object actionValue = resolveContentValue(dto, actionField);
            condition = conditionValue != null ? conditionValue.toString() : "";
            action = actionValue != null ? actionValue.toString() : "";
        }

        Map<String, Object> resultData = new LinkedHashMap<>();
        resultData.put("condition", condition);
        resultData.put("action", action);
        resultData.put("entityFqn", entityFqn);

        return CognitionResult.success(operatorId(), category(), resultData);
    }
}
