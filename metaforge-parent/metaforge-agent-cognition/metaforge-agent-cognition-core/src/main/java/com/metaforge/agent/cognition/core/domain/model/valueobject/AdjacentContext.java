package com.metaforge.agent.cognition.core.domain.model.valueobject;

import java.util.List;

public record AdjacentContext(
        List<String> previousSteps,
        List<String> nextSteps,
        List<String> upstreamEntities,
        List<String> downstreamEntities) {
}
