package com.metaforge.agent.cognition.core.domain.service;

import com.metaforge.agent.cognition.core.domain.model.aggregate.GuidanceResult;
import java.util.List;
import java.util.Map;

public interface PerspectiveOrchestrationService {

    GuidanceResult orchestrate(TemplateResolutionService.ExecutionPlan executionPlan, OrchestrationContext context);

    record OrchestrationContext(
            List<String> bundleFqns,
            String entityFqn,
            List<String> entityTypes,
            String subjectDomainFqn,
            Map<String, String> contextParameters,
            String cursor,
            Integer pageSize,
            String expand,
            List<String> narrowedEntityFqns,
            List<String> narrowedSchemaFqns) {}
}
