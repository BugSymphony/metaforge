package com.metaforge.graph.domain.model.valueobject;

import java.util.Objects;

/**
 * RelationSchemaFQN 值对象——绑定的元模型 RelationSchema FQN（含版本号）。
 */
public final class RelationSchemaFQN {

    private final String value;

    private RelationSchemaFQN(String value) {
        this.value = value;
    }

    public static RelationSchemaFQN of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("RelationSchema FQN 不能为空");
        }
        return new RelationSchemaFQN(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RelationSchemaFQN that)) return false;
        return Objects.equals(value, that.value);
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
