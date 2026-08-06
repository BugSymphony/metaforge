package com.metaforge.computeengine.api.dto.common;

import java.io.Serializable;

/**
 * 实体摘要。
 *
 * <p>查询结果中内联的实体精简信息，以 FQN 为核心标识，包含展示名与元模型类型 FQN。
 *
 * @param fqn             实体 FQN
 * @param name            展示名
 * @param entitySchemaFqn 元模型 EntitySchema FQN
 * @author metaforge
 */
public record EntitySummary(
        String fqn,
        String name,
        String entitySchemaFqn
) implements Serializable {
}
