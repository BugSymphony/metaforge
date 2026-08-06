package com.metaforge.graph.api.enums;

/**
 * 关系变更类型枚举——表征关系实例生命周期中的变更动作。
 */
public enum ChangeType {

    /** 生效——草稿升级为主表正式版本 */
    ACTIVATED("生效"),

    /** 下线——主表生效版本移除，归档至历史表 */
    DEPRECATED("下线");

    private final String displayName;

    ChangeType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
