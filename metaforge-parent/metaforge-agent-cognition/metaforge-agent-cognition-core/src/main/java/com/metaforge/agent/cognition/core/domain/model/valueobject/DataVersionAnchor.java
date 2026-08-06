package com.metaforge.agent.cognition.core.domain.model.valueobject;

import java.time.Instant;

public record DataVersionAnchor(
        String bundleFqn,
        String publishedVersionFqn,
        int latestVersionNumber,
        Instant queriedAt) {
}
