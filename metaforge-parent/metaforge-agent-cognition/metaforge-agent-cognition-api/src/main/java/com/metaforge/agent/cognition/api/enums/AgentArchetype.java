package com.metaforge.agent.cognition.api.enums;

public enum AgentArchetype {

    EXECUTION,
    EXPLORATION,
    AUDIT,
    ORCHESTRATION;

    public static AgentArchetype fromString(String value) {
        if (value == null) return EXECUTION;
        try { return valueOf(value.toUpperCase()); } catch (IllegalArgumentException e) { return EXECUTION; }
    }
}
