package com.metaforge.agent.cognition.core.domain.service;

import com.metaforge.agent.cognition.api.enums.*;
import com.metaforge.agent.cognition.core.domain.model.valueobject.TemplateId;

import java.util.List;

public interface TemplateResolutionService {

    ExecutionPlan resolve(TemplateId templateId, RequestOverrides overrides);

    ExecutionPlan resolveFromRequest(String cognitionDepth, String agentArchetype,
                                      List<String> perspectives, Integer maxTokens);

    record ExecutionPlan(
            List<PerspectiveCode> perspectives,
            CognitionDepth depth,
            AgentArchetype archetype,
            int maxTokens,
            OutputFormat outputFormat,
            ContextMode contextMode,
            ScopeMode scopeMode) {}

    record RequestOverrides(
            List<String> perspectives,
            String cognitionDepth,
            String agentArchetype,
            Integer maxTokens,
            String outputFormat,
            ContextMode contextMode,
            ScopeMode scopeMode) {}
}
