package com.metaforge.agent.cognition.api.enums;

public enum CognitionDepth {

    L1(3),
    L2(7),
    L3(14);

    private final int maxPerspectives;

    CognitionDepth(int maxPerspectives) {
        this.maxPerspectives = maxPerspectives;
    }

    public int maxPerspectives() { return maxPerspectives; }

    public static CognitionDepth fromString(String value) {
        if (value == null) return L2;
        try { return valueOf(value.toUpperCase()); } catch (IllegalArgumentException e) { return L2; }
    }
}
