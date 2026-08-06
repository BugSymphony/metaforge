package com.metaforge.metadata.api.constants;

/**
 * metadata-management BC 错误码常量，范围 31000-31099。
 */
public final class MetadataErrorCodes {

    private MetadataErrorCodes() {
        throw new UnsupportedOperationException("常量类不可实例化");
    }

    /** FQN 全局唯一性冲突 */
    public static final int FQN_CONFLICT = 31001;

    /** FQN segment 不符合文法 */
    public static final int FQN_INVALID_SEGMENT = 31002;

    /** JSON Schema 结构校验失败 */
    public static final int SCHEMA_VALIDATION_FAILED = 31003;

    /** 生效元数据不存在 */
    public static final int ENTITY_NOT_FOUND = 31004;

    /** 草稿不存在 */
    public static final int DRAFT_NOT_FOUND = 31005;

    /** 历史版本不存在 */
    public static final int VERSION_NOT_FOUND = 31006;

    /** 生效操作失败 */
    public static final int ACTIVATION_FAILED = 31007;

    /** 下线操作被拦截（存在活跃引用/生效子实体） */
    public static final int DEACTIVATION_BLOCKED = 31008;

    /** 父实体未生效（子实体创建拦截） */
    public static final int PARENT_NOT_ACTIVE = 31009;

    /** 绑定元模型版本未发布 */
    public static final int SCHEMA_VERSION_NOT_PUBLISHED = 31010;

    /** 导入文件解析失败 */
    public static final int IMPORT_PARSE_FAILED = 31011;

    /** FQN segment 包含保留分隔符 */
    public static final int FQN_SEGMENT_RESERVED_CHAR = 31012;
}
