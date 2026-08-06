package com.metaforge.computeengine.api.enums;

/**
 * 匹配模式枚举。
 *
 * <p>定义 FQN 过滤条件中字符串匹配策略。
 * PREFIX 为前缀模糊匹配，EXACT 为精确匹配，PATTERN 为 SQL LIKE 模式匹配（仅 relationInstanceFqns 支持）。
 *
 * @author metaforge
 */
public enum MatchMode {

    PREFIX("前缀匹配"),
    EXACT("精确匹配"),
    PATTERN("模式匹配");

    private final String description;

    MatchMode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
