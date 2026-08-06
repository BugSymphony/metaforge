package com.metaforge.computeengine.domain.model.valueobject;

/**
 * 图模式中的单个路径段（Entity —[Relation]—> Entity）。
 *
 * @param sourceEntityType 源实体类型（完整 FQN 或 '*' 通配）
 * @param relationType     关系类型（完整 FQN 或 '?' 通配）
 * @param targetEntityType 目标实体类型（完整 FQN 或 '*' 通配）
 * @author metaforge
 */
public record PatternSegment(
        String sourceEntityType,
        String relationType,
        String targetEntityType
) {
}
