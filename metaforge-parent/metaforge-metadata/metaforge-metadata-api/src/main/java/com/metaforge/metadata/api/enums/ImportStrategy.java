package com.metaforge.metadata.api.enums;

/**
 * 批量导入幂等策略。
 */
public enum ImportStrategy {

    /** 跳过已存在的 FQN */
    SKIP,

    /** 遇冲突报错 */
    ERROR
}
