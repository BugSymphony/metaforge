package com.metaforge.agent.cognition.api.dto.request;

public record NavigateRequest(
        String anchorFqn,
        String level,
        String cursor,
        Integer pageSize,
        String expand,
        String format) {
}
