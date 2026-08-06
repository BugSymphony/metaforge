package com.metaforge.computeengine.api.enums;

/**
 * 权重策略枚举。
 *
 * <p>定义路径推理中多跳权重的聚合计算策略。
 *
 * @author metaforge
 */
public enum WeightStrategy {

    MULTIPLY("连乘"),
    ADD("累加"),
    MAX("取最大"),
    NONE("无权重计算");

    private final String description;

    WeightStrategy(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
