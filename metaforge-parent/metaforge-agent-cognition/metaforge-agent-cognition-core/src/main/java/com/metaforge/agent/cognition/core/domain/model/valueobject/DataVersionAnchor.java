package com.metaforge.agent.cognition.core.domain.model.valueobject;

import java.time.Instant;

public record DataVersionAnchor(String bundleFqn, String versionFqn, Instant resolvedAt) {

    public DataVersionAnchor {
        if (bundleFqn == null || bundleFqn.isBlank()) {
            throw new IllegalArgumentException("bundleFqn must not be blank");
        }
        if (versionFqn == null || versionFqn.isBlank()) {
            throw new IllegalArgumentException("versionFqn must not be blank");
        }
        if (resolvedAt == null) {
            resolvedAt = Instant.now();
        }
    }
}
