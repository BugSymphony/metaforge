package com.metaforge.computeengine.api.enums;

/**
 * 关联类型枚举。
 *
 * <p>定义语义关系网络中实体间的关联类别，每种类型具有不同的传导语义和遍历行为。
 *
 * @author metaforge
 */
public enum AssociationType {

    COMPOSITION("整体-部分层级关系"),
    DEPENDENCY_INFLUENCE("依赖/影响关系"),
    PROCESS_SEQUENCE("流程序列关系"),
    ASSOCIATION_REFERENCE("关联引用关系"),
    MAPPING_CORRESPONDENCE("映射对应关系");

    private final String description;

    AssociationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
