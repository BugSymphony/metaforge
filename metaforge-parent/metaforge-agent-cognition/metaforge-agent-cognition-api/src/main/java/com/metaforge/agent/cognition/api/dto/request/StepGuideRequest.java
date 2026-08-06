package com.metaforge.agent.cognition.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StepGuideRequest(
        @NotBlank(message = "entity_fqn 不能为空")
        String entityFqn,
        @Size(max = 32, message = "cognition_depth 长度不能超过 32")
        String cognitionDepth,
        @Size(max = 32, message = "agent_archetype 长度不能超过 32")
        String agentArchetype,
        Integer maxTokens,
        @Size(max = 32, message = "format 长度不能超过 32")
        String format) {
}
