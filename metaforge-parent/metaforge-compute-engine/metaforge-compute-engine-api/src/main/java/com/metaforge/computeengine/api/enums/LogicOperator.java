package com.metaforge.computeengine.api.enums;

/**
 * 逻辑运算符枚举。
 *
 * <p>定义多条件复合检索中属性条件间的组合逻辑。
 *
 * @author metaforge
 */
public enum LogicOperator {

    AND("与"),
    OR("或");

    private final String description;

    LogicOperator(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
