package com.metaforge.agent.cognition.api.dto.request;

import java.util.List;
import java.util.Map;

public record TaskBriefRequest(
        List<String> bundleFqns,
        String cognitionDepth,
        String agentArchetype,
        Integer maxTokens,
        Map<String, String> contextParameters,
        String format) {
}
