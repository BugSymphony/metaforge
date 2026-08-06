package com.metaforge.agent.cognition.api.enums;

public enum ScopeMode {
    INHERITED,
    PURE;

    public static ScopeMode fromString(String value) {
        if (value == null) return PURE;
        try { return valueOf(value.toUpperCase()); } catch (IllegalArgumentException e) { return PURE; }
    }
}
