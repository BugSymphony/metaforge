package com.metaforge.graph.domain.model.valueobject;

import com.metaforge.graph.api.constant.GraphConstants;
import java.util.Objects;

/**
 * FQN 值对象——关系实例的全局唯一标识。
 * 格式：{源实体FQN}#{关系类型FQN}#{目标实体FQN}
 */
public final class FQN {

    private final String value;

    private FQN(String value) {
        this.value = value;
    }

    public static FQN of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("FQN 不能为空");
        }
        if (value.length() > GraphConstants.MAX_FQN_LENGTH) {
            throw new IllegalArgumentException("FQN 长度超过最大限制 " + GraphConstants.MAX_FQN_LENGTH);
        }
        return new FQN(value);
    }

    public String getValue() {
        return value;
    }

    public EntityFQN getSourceEntityFqn() {
        String[] parts = parseParts();
        return EntityFQN.of(parts[0]);
    }

    public String getRelationTypeFqn() {
        String[] parts = parseParts();
        return parts[1];
    }

    public EntityFQN getTargetEntityFqn() {
        String[] parts = parseParts();
        return EntityFQN.of(parts[2]);
    }

    private String[] parseParts() {
        String[] parts = value.split(GraphConstants.FQN_DELIMITER);
        if (parts.length != 3) {
            throw new IllegalArgumentException("FQN 格式不合法，期望三段式（源#类型#目标）: " + value);
        }
        return parts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FQN fqn)) return false;
        return Objects.equals(value, fqn.value);
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
