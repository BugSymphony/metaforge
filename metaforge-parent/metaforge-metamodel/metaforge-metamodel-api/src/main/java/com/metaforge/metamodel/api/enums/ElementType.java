package com.metaforge.metamodel.api.enums;

/**
 * 元素类型枚举，对应 FQN 类型前缀。
 * 用于 API 层类型前缀的解析与生成。
 */
public enum ElementType {

    /** Bundle */
    BUNDLE("bundle"),

    /** BundleVersion */
    BUNDLE_VERSION("version"),

    /** Package */
    PACKAGE("package"),

    /** EntitySchema */
    ENTITY_SCHEMA("entity"),

    /** RelationSchema */
    RELATION_SCHEMA("relation"),

    /** AttributeTemplate */
    ATTRIBUTE_TEMPLATE("template");

    private final String prefix;

    ElementType(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    /**
     * 根据类型前缀查找枚举值。
     *
     * @param prefix 类型前缀（如 "entity", "relation"）
     * @return 对应的枚举值，如果未匹配则返回 null
     */
    public static ElementType fromPrefix(String prefix) {
        for (ElementType type : values()) {
            if (type.prefix.equals(prefix)) {
                return type;
            }
        }
        return null;
    }
}
