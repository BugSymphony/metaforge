package com.metaforge.computeengine.api.enums;

/**
 * 截断原因枚举。
 *
 * <p>当查询结果因达到约束上限而被截断时，标记截断的具体原因。
 *
 * @author metaforge
 */
public enum TruncatedReason {

    DEPTH_EXCEEDED("遍历深度超限"),
    COUNT_EXCEEDED("结果数量超限"),
    TIMEOUT("查询超时中断");

    private final String description;

    TruncatedReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
