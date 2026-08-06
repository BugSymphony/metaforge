package com.metaforge.metamodel.api.enums;

/**
 * 版本状态枚举：草稿 → 已发布，正向不可逆。
 */
public enum VersionStatus {

    /** 草稿态：全字段可编辑 */
    DRAFT,

    /** 已发布态：全字段只读冻结 */
    PUBLISHED
}
