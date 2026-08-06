package com.metaforge.agent.cognition.api.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public record CognitionRequest(
        @Size(max = 20, message = "bundle_fqns 最多支持 20 个")
        List<String> bundleFqns,
        @Size(max = 14, message = "perspectives 最多支持 14 个")
        List<String> perspectives,
        @Size(max = 512, message = "entity_fqn 长度不能超过 512")
        String entityFqn,
        @Size(max = 50, message = "entity_types 最多支持 50 个")
        List<String> entityTypes,
        @Size(max = 512, message = "subject_domain_fqn 长度不能超过 512")
        String subjectDomainFqn,
        @Size(max = 32, message = "scope_mode 长度不能超过 32")
        String scopeMode,
        @Size(max = 32, message = "cognition_depth 长度不能超过 32")
        String cognitionDepth,
        @Size(max = 32, message = "agent_archetype 长度不能超过 32")
        String agentArchetype,
        Integer maxTokens,
        @Size(max = 32, message = "expand 长度不能超过 32")
        String expand,
        @Size(max = 32, message = "format 长度不能超过 32")
        String format,
        @Size(max = 256, message = "cursor 长度不能超过 256")
        String cursor,
        Integer pageSize,
        Map<String, String> contextParameters) {
}
