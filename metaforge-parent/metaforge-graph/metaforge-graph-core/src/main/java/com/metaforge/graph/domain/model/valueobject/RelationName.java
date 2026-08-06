package com.metaforge.graph.domain.model.valueobject;

import com.metaforge.graph.api.constant.GraphConstants;
import java.util.Objects;

/**
 * RelationName 值对象——关系的人类可读名称（≤512 字符）。
 */
public final class RelationName {

    private final String value;

    private RelationName(String value) {
        this.value = value;
    }

    public static RelationName of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("关系名称不能为空");
        }
        if (value.length() > GraphConstants.MAX_RELATION_NAME_LENGTH) {
            throw new IllegalArgumentException("关系名称长度超过最大限制 " + GraphConstants.MAX_RELATION_NAME_LENGTH);
        }
        return new RelationName(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RelationName that)) return false;
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
