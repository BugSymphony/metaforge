package com.metaforge.metadata.api.enums;

/**
 * 导出格式枚举。
 */
public enum ExportFormat {

    JSON("JSON格式"),
    YAML("YAML格式");

    private final String description;

    ExportFormat(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
