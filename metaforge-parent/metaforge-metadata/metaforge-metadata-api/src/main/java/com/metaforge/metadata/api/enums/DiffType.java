package com.metaforge.metadata.api.enums;

/**
 * 差异类型枚举。
 */
public enum DiffType {

    ADDED("新增"),
    MODIFIED("修改"),
    DELETED("删除");

    private final String description;

    DiffType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
