package com.metaforge.agent.cognition.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SubTaskBriefRequest(
        @NotBlank(message = "entry_entity_fqn 不能为空")
        String entryEntityFqn,
        @NotBlank(message = "scope_mode 为必填字段，必须为 INHERITED 或 PURE")
        String scopeMode,
        @Size(max = 32, message = "cognition_depth 长度不能超过 32")
        String cognitionDepth,
        @Size(max = 32, message = "agent_archetype 长度不能超过 32")
        String agentArchetype,
        Integer maxTokens,
        @Size(max = 14, message = "perspectives 最多支持 14 个")
        List<String> perspectives,
        @Size(max = 32, message = "format 长度不能超过 32")
        String format) {
}
