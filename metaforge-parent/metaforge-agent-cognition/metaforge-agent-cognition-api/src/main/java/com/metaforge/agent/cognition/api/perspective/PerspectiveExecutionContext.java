package com.metaforge.agent.cognition.api.perspective;

import com.metaforge.agent.cognition.api.enums.ContextMode;
import java.util.List;
import java.util.Map;

public record PerspectiveExecutionContext(
        ContextMode contextMode,
        List<String> bundleFqns,
        String entityFqn,
        List<String> entityTypes,
        String subjectDomainFqn,
        Map<String, String> contextParameters,
        String cursor,
        Integer pageSize,
        String expand,
        List<String> narrowedEntityFqns,
        List<String> narrowedSchemaFqns) {
}
