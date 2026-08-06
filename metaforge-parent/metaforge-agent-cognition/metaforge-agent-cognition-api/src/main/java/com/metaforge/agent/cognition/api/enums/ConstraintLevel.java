package com.metaforge.agent.cognition.api.enums;

public enum ConstraintLevel {
    MANDATORY,
    RECOMMENDED,
    REFERENCE;

    public static ConstraintLevel fromString(String value) {
        if (value == null) return REFERENCE;
        try { return valueOf(value.toUpperCase()); } catch (IllegalArgumentException e) { return REFERENCE; }
    }
}
