package com.metaforge.agent.cognition.operator.capability;

import com.metaforge.agent.cognition.api.enums.DimensionCategory;
import com.metaforge.agent.cognition.api.spi.CognitionQueryContext;
import com.metaforge.agent.cognition.api.spi.CognitionResult;
import com.metaforge.agent.cognition.operator.common.AbstractCognitionOperator;
import com.metaforge.metadata.api.dto.response.MetadataEntityDto;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class CapabilityCallMethodOperator extends AbstractCognitionOperator {

    private static final Set<String> VALID_METHODS = Set.of("REST", "MCP", "CLI", "LOCALMETHOD", "INTERNAL");

    @Override
    public String operatorId() {
        return "capability.call-method";
    }

    @Override
    public DimensionCategory category() {
        return DimensionCategory.CAPABILITY;
    }

    @Override
    public CognitionResult execute(CognitionQueryContext context) {
        String entityFqn = context.entityFqn();
        if (entityFqn == null || entityFqn.isBlank()) {
            return wrapFailure("缺少 entityFqn 参数");
        }

        String contentField = configString(context.operatorConfig(), "contentField", "call_method");

        Object portResult = executeWithPort(() -> metadataReadPort.getByFqn(entityFqn));
        if (portResult instanceof CognitionResult cr) return cr;

        Object rawValue = null;
        if (portResult instanceof MetadataEntityDto dto) {
            rawValue = resolveContentValue(dto, contentField);
        }

        String callMethod = resolveCallMethod(rawValue);

        Map<String, Object> resultData = new LinkedHashMap<>();
        resultData.put("callMethod", callMethod);
        resultData.put("entityFqn", entityFqn);
        resultData.put("rawValue", rawValue);

        return CognitionResult.success(operatorId(), category(), resultData);
    }

    private String resolveCallMethod(Object value) {
        if (value instanceof String str && !str.isBlank()) {
            String upper = str.toUpperCase(Locale.ROOT);
            if (VALID_METHODS.contains(upper)) {
                return str;
            }
        }
        return "LocalMethod";
    }
}
