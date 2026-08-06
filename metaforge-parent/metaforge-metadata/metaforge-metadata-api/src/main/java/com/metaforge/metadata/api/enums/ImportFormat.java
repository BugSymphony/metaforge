package com.metaforge.metadata.api.enums;

/**
 * 导入格式枚举。
 */
public enum ImportFormat {

    JSON("JSON格式"),
    YAML("YAML格式");

    private final String description;

    ImportFormat(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
