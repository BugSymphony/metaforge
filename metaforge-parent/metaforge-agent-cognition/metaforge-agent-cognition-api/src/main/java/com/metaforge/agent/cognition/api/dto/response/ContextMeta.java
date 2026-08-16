package com.metaforge.agent.cognition.api.dto.response;

import com.metaforge.agent.cognition.api.dto.request.Scope;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

public record ContextMeta(
        String template,
        List<String> versionAnchors,
        Scope scopeApplied,
        Integer tokenEstimate,
        Instant generatedAt,
        List<String> skippedEntities,
        List<String> truncatedPerspectives
) {

    public static ContextMeta empty(String templateId) {
        return new ContextMeta(
                templateId,
                Collections.emptyList(),
                Scope.EMPTY,
                0,
                Instant.now(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }
}
