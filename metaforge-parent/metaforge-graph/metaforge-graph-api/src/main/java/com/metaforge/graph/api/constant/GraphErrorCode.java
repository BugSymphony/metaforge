package com.metaforge.graph.api.constant;

/**
 * metaforge-graph BC 错误码常量定义。
 *
 * <p>错误码范围 32001-32015，为 semantic-relation-network BC 独占分配范围。
 */
public final class GraphErrorCode {

    private GraphErrorCode() {
        throw new UnsupportedOperationException("常量类不可实例化");
    }

    /** FQN 全局唯一性冲突 */
    public static final int FQN_CONFLICT = 32001;

    /** 关系实例不存在 */
    public static final int RELATION_NOT_FOUND = 32002;

    /** 草稿不存在 */
    public static final int DRAFT_NOT_FOUND = 32003;

    /** 草稿已存在，不允许重复创建 */
    public static final int DRAFT_ALREADY_EXISTS = 32004;

    /** JSON Schema 结构校验失败 */
    public static final int SCHEMA_VALIDATION_FAILED = 32005;

    /** 基数约束超限 */
    public static final int CARDINALITY_EXCEEDED = 32006;

    /** 端点实体无效（不存在或已下线） */
    public static final int ENDPOINT_INVALID = 32007;

    /** 下游强依赖阻断下线 */
    public static final int DEPENDENCY_BLOCKING = 32008;

    /** 草稿编辑内容不可变更字段（fqn/schema/端点） */
    public static final int DRAFT_IMMUTABLE_FIELD = 32009;

    /** 关联 RelationSchema 未发布 */
    public static final int SCHEMA_NOT_PUBLISHED = 32010;

    /** 批量导入 content 超限（>10MB） */
    public static final int CONTENT_SIZE_EXCEEDED = 32011;

    /** 历史版本不存在 */
    public static final int VERSION_NOT_FOUND = 32012;

    /** FQN 格式不合法 */
    public static final int FQN_FORMAT_INVALID = 32013;

    /** 跨域关系未授权 */
    public static final int CROSS_DOMAIN_UNAUTHORIZED = 32014;

    /** 关系实例状态非法（状态机违规） */
    public static final int ILLEGAL_STATE = 32015;
}
