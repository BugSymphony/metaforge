package com.metaforge.graph.api.enums;

/**
 * 关系状态枚举——草稿 → 生效 → 下线，正向不可逆。
 */
public enum RelationStatus {

    /** 草稿态：仅存在于草稿表，对外不可见 */
    DRAFT("草稿"),

    /** 生效态：存在于主表，对外默认可见 */
    ACTIVE("生效"),

    /** 下线态：主表已移除，历史表保留完整归档 */
    DEPRECATED("下线");

    private final String displayName;

    RelationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
