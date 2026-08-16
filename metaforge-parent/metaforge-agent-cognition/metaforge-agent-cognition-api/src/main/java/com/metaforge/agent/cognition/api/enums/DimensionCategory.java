package com.metaforge.agent.cognition.api.enums;

public enum DimensionCategory {

    ONTOLOGICAL("本体论", "object"),
    STRUCTURAL("结构论", "object"),
    RELATIONAL("关系论", "object"),
    PROCEDURAL("流程论", "object"),
    DEONTIC("约束论", "action"),
    CAPABILITY("能力论", "action"),
    EPISTEMIC("认知论", "meta"),
    GOVERNANCE("治理", "meta");

    private final String displayName;
    private final String layer;

    DimensionCategory(String displayName, String layer) {
        this.displayName = displayName;
        this.layer = layer;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getLayer() {
        return layer;
    }
}
