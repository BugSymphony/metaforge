package com.metaforge.graph.domain.model.valueobject;

import com.metaforge.graph.api.constant.GraphConstants;
import java.util.Objects;

/**
 * EntityFQN 值对象——上游元数据实体的 FQN 标识。
 */
public final class EntityFQN {

    private final String value;

    private EntityFQN(String value) {
        this.value = value;
    }

    public static EntityFQN of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("实体 FQN 不能为空");
        }
        if (value.length() > GraphConstants.MAX_ENTITY_FQN_LENGTH) {
            throw new IllegalArgumentException("实体 FQN 长度超过最大限制 " + GraphConstants.MAX_ENTITY_FQN_LENGTH);
        }
        return new EntityFQN(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntityFQN entityFqn)) return false;
        return Objects.equals(value, entityFqn.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
