package com.metaforge.graph.api.constant;

/**
 * metaforge-graph BC 业务常量定义。
 */
public final class GraphConstants {

    private GraphConstants() {
        throw new UnsupportedOperationException("常量类不可实例化");
    }

    /** 关系 content 字段最大大小（字节）：10MB */
    public static final long MAX_CONTENT_SIZE = 10 * 1024 * 1024L;

    /** FQN 分隔符 */
    public static final String FQN_DELIMITER = "#";

    /** 默认分页页码 */
    public static final int DEFAULT_PAGE_NUMBER = 0;

    /** 默认分页大小 */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** 分页大小最大值 */
    public static final int MAX_PAGE_SIZE = 200;

    /** FQN 最大长度 */
    public static final int MAX_FQN_LENGTH = 1536;

    /** 关系名称最大长度 */
    public static final int MAX_RELATION_NAME_LENGTH = 512;

    /** 实体 FQN 最大长度 */
    public static final int MAX_ENTITY_FQN_LENGTH = 512;

    /** RelationSchema FQN 最大长度 */
    public static final int MAX_SCHEMA_FQN_LENGTH = 256;

    /** 关系类型枚举字符串最大长度 */
    public static final int MAX_RELATION_TYPE_LENGTH = 64;

    /** 索引方向：出边 */
    public static final String DIRECTION_OUTBOUND = "OUTBOUND";

    /** 索引方向：入边 */
    public static final String DIRECTION_INBOUND = "INBOUND";
}
