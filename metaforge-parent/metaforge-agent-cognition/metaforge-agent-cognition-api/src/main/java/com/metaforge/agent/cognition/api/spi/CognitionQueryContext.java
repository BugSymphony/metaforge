package com.metaforge.agent.cognition.api.spi;

import com.metaforge.agent.cognition.api.dto.request.Scope;
import com.metaforge.agent.cognition.api.enums.AgentArchetype;
import com.metaforge.agent.cognition.api.enums.CognitionDepth;
import com.metaforge.agent.cognition.api.enums.DimensionCategory;

import java.util.List;
import java.util.Map;

public record CognitionQueryContext(
        String templateId,
        String operatorId,
        DimensionCategory category,
        Scope scope,
        List<String> bundleFqns,
        String entityFqn,
        Map<String, Object> templateParams,
        AgentArchetype agentArchetype,
        CognitionDepth cognitionDepth,
        Integer cursor,
        int pageSize,
        Map<String, Object> templateConfig,
        Map<String, Object> operatorConfig
) {}
