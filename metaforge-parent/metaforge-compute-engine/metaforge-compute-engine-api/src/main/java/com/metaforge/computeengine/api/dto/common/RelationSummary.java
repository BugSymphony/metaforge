package com.metaforge.computeengine.api.dto.common;

import com.metaforge.computeengine.api.enums.AssociationType;

import java.io.Serializable;

/**
 * 关系摘要。
 *
 * <p>查询结果中内联的关系精简信息，以 FQN 为核心标识关联类型和端点 FQN。
 *
 * @param fqn             关系实例 FQN
 * @param associationType 关联类型
 * @param sourceEntityFqn 源实体 FQN
 * @param targetEntityFqn 目标实体 FQN
 * @author metaforge
 */
public record RelationSummary(
        String fqn,
        AssociationType associationType,
        String sourceEntityFqn,
        String targetEntityFqn
) implements Serializable {
}
