package com.metaforge.metadata.api.enums;

/**
 * 匹配模式枚举。
 */
public enum MatchMode {

    EXACT("精确匹配"),
    PREFIX("前缀匹配");

    private final String description;

    MatchMode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
