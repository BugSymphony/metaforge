package com.metaforge.agent.cognition.api.dto;

import java.time.Instant;

public record DataVersionAnchor(
        String bundleFqn,
        String publishedVersionFqn,
        Integer latestVersionNumber,
        Instant queriedAt) {
}
