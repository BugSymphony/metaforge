package com.metaforge.agent.cognition.api.dto.request;

import com.metaforge.agent.cognition.api.enums.AgentArchetype;
import com.metaforge.agent.cognition.api.enums.CognitionDepth;
import com.metaforge.agent.cognition.api.enums.OutputFormat;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

public record CognitionRequest(
        Scope scope,
        Map<String, Object> params,
        String format,
        CognitionDepth cognitionDepth,
        AgentArchetype agentArchetype,
        Integer maxTokens
) {

    public static CognitionRequest withDefaults() {
        return new CognitionRequest(
                Scope.EMPTY,
                Collections.emptyMap(),
                "JSON",
                CognitionDepth.L2,
                AgentArchetype.EXECUTION,
                8000
        );
    }

    public CognitionRequest {
        scope = scope != null ? scope : Scope.EMPTY;
        params = params != null ? params : Collections.emptyMap();
        format = format != null ? format : "JSON";
        cognitionDepth = cognitionDepth != null ? cognitionDepth : CognitionDepth.L2;
        agentArchetype = agentArchetype != null ? agentArchetype : AgentArchetype.EXECUTION;
        maxTokens = maxTokens != null ? maxTokens : 8000;
    }

    public OutputFormat resolvedFormat() {
        if (format == null || format.isBlank()) {
            return OutputFormat.JSON;
        }
        try {
            return OutputFormat.valueOf(format.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
