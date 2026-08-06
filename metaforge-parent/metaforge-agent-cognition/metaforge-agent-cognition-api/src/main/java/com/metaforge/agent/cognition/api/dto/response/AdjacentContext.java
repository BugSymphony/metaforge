package com.metaforge.agent.cognition.api.dto.response;

import java.util.List;

public record AdjacentContext(
        List<String> previousSteps,
        List<String> nextSteps,
        List<String> upstreamEntities,
        List<String> downstreamEntities) {
}
