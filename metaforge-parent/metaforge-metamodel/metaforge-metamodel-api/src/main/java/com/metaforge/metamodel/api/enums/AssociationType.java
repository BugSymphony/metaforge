package com.metaforge.metamodel.api.enums;

/**
 * 关联类型枚举，定义 RelationSchema 的关联语义。
 * 限定为内置枚举值，不可自定义。
 */
public enum AssociationType {

    /** 组成关系 */
    COMPOSITION("组成"),

    /** 关联引用 */
    ASSOCIATION_REFERENCE("关联引用"),

    /** 映射对应 */
    MAPPING_CORRESPONDENCE("映射对应"),

    /** 依赖影响 */
    DEPENDENCY_INFLUENCE("依赖影响"),

    /** 流程时序 */
    PROCESS_SEQUENCE("流程时序");

    private final String displayName;

    AssociationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 根据显示名称查找枚举值。
     *
     * @param displayName 关联类型显示名（如 "组成"、"关联引用"）
     * @return 对应的枚举值
     * @throws IllegalArgumentException 如果显示名不匹配任何枚举值
     */
    public static AssociationType fromDisplayName(String displayName) {
        if (displayName == null) {
            throw new IllegalArgumentException("关联类型不能为空");
        }
        for (AssociationType type : values()) {
            if (type.displayName.equals(displayName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的关联类型: " + displayName);
    }
}
