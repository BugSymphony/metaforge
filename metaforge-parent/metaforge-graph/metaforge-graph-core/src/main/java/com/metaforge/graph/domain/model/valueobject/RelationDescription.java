package com.metaforge.graph.domain.model.valueobject;

import java.util.Objects;

/**
 * RelationDescription 值对象——关系的文本描述。
 */
public final class RelationDescription {

    private final String value;

    private RelationDescription(String value) {
        this.value = value;
    }

    public static RelationDescription of(String value) {
        return new RelationDescription(value);
    }

    public static RelationDescription empty() {
        return new RelationDescription(null);
    }

    public String getValue() {
        return value;
    }

    public boolean isEmpty() {
        return value == null || value.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RelationDescription that)) return false;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value != null ? value : "";
    }
}
