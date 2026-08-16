package com.metaforge.agent.cognition.core.domain.model.valueobject;

import java.util.Objects;
import java.util.regex.Pattern;

public record TemplateId(String value) {

    private static final Pattern PATTERN = Pattern.compile("[A-Z][A-Z0-9_]+");

    public TemplateId {
        Objects.requireNonNull(value, "templateId must not be null");
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "templateId 格式无效: '" + value + "'，须满足 [A-Z][A-Z0-9_]+");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
