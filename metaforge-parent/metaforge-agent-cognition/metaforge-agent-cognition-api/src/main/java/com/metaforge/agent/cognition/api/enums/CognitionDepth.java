package com.metaforge.agent.cognition.api.enums;

public enum CognitionDepth {

    L1(0.33),
    L2(0.67),
    L3(1.0);

    private final double trimRatio;

    CognitionDepth(double trimRatio) {
        this.trimRatio = trimRatio;
    }

    public double getTrimRatio() {
        return trimRatio;
    }
}
