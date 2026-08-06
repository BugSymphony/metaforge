package com.metaforge.metamodel.api.enums;

/**
 * 升级等级枚举（SemVer 2.0 兼容）。
 */
public enum UpgradeLevel {

    /** 主版本升级：不兼容的元模型变更 */
    MAJOR,

    /** 次版本升级：向后兼容的功能新增 */
    MINOR,

    /** 修订版本升级：非结构性修正 */
    PATCH
}
