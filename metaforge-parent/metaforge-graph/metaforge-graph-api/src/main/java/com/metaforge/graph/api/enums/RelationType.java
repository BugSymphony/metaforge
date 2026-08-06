package com.metaforge.graph.api.enums;

/**
 * 关系类型枚举——定义语义关系网络中支持的五种关系语义。
 */
public enum RelationType {

    /** 组成——整体与部分的结构化关系 */
    COMPOSITION("组成"),

    /** 关联引用——实体间的通用引用关系 */
    ASSOCIATION_REFERENCE("关联引用"),

    /** 映射对应——不同领域实体间的映射 */
    MAPPING_CORRESPONDENCE("映射对应"),

    /** 依赖影响——依赖与影响链路 */
    DEPENDENCY_INFLUENCE("依赖影响"),

    /** 流程时序——流程中的先后顺序 */
    PROCESS_SEQUENCE("流程时序");

    private final String displayName;

    RelationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static RelationType fromDisplayName(String displayName) {
        if (displayName == null) {
            throw new IllegalArgumentException("关系类型不能为空");
        }
        for (RelationType type : values()) {
            if (type.displayName.equals(displayName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的关系类型: " + displayName);
    }
}
