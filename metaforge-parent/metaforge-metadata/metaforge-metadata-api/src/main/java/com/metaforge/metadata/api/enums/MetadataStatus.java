package com.metaforge.metadata.api.enums;

/**
 * 元数据生命周期状态枚举。
 */
public enum MetadataStatus {

    DRAFT("草稿"),
    ACTIVE("生效"),
    DEPRECATED("已下线");

    private final String description;

    MetadataStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
