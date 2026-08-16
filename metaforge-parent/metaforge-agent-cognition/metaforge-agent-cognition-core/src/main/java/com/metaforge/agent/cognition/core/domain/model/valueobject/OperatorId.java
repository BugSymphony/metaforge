package com.metaforge.agent.cognition.core.domain.model.valueobject;

import java.util.Objects;
import java.util.regex.Pattern;

public record OperatorId(String value) {

    private static final Pattern PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9._-]+");

    public OperatorId {
        Objects.requireNonNull(value, "operatorId must not be null");
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "operatorId 格式无效: '" + value + "'，须满足 {prefix}.{name} 格式");
        }
        if (!value.contains(".")) {
            throw new IllegalArgumentException(
                    "operatorId 缺少分类前缀: '" + value + "'");
        }
    }

    public String prefix() {
        return value.substring(0, value.indexOf('.'));
    }

    public String name() {
        return value.substring(value.indexOf('.') + 1);
    }

    @Override
    public String toString() {
        return value;
    }
}
