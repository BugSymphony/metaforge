package com.metaforge.metadata.api.constants;

/**
 * 元数据生命周期状态常量。
 */
public final class MetadataStatusConstants {

    private MetadataStatusConstants() {
        throw new UnsupportedOperationException("常量类不可实例化");
    }

    /** 草稿态 */
    public static final String DRAFT = "DRAFT";

    /** 生效态 */
    public static final String ACTIVE = "ACTIVE";

    /** 已下线 */
    public static final String DEPRECATED = "DEPRECATED";

    /** 历史归档态（历史版本表） */
    public static final String HISTORY = "HISTORY";
}
