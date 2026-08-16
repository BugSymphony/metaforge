package com.metaforge.agent.cognition.operator.deontic;

import com.metaforge.agent.cognition.api.enums.DimensionCategory;
import com.metaforge.agent.cognition.api.spi.CognitionQueryContext;
import com.metaforge.agent.cognition.api.spi.CognitionResult;
import com.metaforge.agent.cognition.operator.common.AbstractCognitionOperator;
import com.metaforge.metadata.api.dto.response.MetadataEntityDto;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Component
public class DeonticLevelClassifierOperator extends AbstractCognitionOperator {

    private static final Set<String> VALID_LEVELS = Set.of("MANDATORY", "RECOMMENDED", "REFERENCE");
    private static final String DEFAULT_LEVEL = "REFERENCE";

    @Override
    public String operatorId() {
        return "deontic.level-classifier";
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

        String contentField = configString(context.operatorConfig(), "contentField", "constraint_level");

        Object portResult = executeWithPort(() -> metadataReadPort.getByFqn(entityFqn));
        if (portResult instanceof CognitionResult cr) return cr;

        Object rawLevel = null;
        if (portResult instanceof MetadataEntityDto dto) {
            rawLevel = resolveContentValue(dto, contentField);
            if (rawLevel == null) {
                rawLevel = resolveContentValue(dto, "level");
            }
        }

        String level = resolveLevel(rawLevel);

        Map<String, Object> resultData = new LinkedHashMap<>();
        resultData.put("level", level);
        resultData.put("entityFqn", entityFqn);
        resultData.put("rawLevel", rawLevel);

        return CognitionResult.success(operatorId(), category(), resultData);
    }

    private String resolveLevel(Object value) {
        if (value instanceof String str && !str.isBlank()) {
            String upper = str.toUpperCase(java.util.Locale.ROOT);
            if (VALID_LEVELS.contains(upper)) {
                return upper;
            }
            if (str.contains("必须") || str.contains("强制") || str.contains("MUST")) {
                return "MANDATORY";
            }
            if (str.contains("建议") || str.contains("推荐") || str.contains("SHOULD")) {
                return "RECOMMENDED";
            }
            return "REFERENCE";
        }
        return DEFAULT_LEVEL;
    }
}
