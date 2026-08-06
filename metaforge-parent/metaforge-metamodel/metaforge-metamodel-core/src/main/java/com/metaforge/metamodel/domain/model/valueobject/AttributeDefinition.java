package com.metaforge.metamodel.domain.model.valueobject;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 属性定义值对象，对应 AttributeTemplate 的 attribute_definitions 项。
 * 不负责 JSON Schema 规范校验（校验在发布环节完成），仅做基本字段验证。
 */
public final class AttributeDefinition {

    private final String name;
    private final String type;
    private final Map<String, Object> constraints;

    private AttributeDefinition(String name, String type, Map<String, Object> constraints) {
        this.name = name;
        this.type = type;
        this.constraints = constraints != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(constraints))
                : Collections.emptyMap();
    }

    public static AttributeDefinition of(String name, String type,
                                          Map<String, Object> constraints) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("属性定义名不能为空");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("属性定义类型不能为空");
        }
        return new AttributeDefinition(name, type, constraints);
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> getConstraints() {
        return constraints;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AttributeDefinition that)) return false;
        return Objects.equals(name, that.name)
                && Objects.equals(type, that.type)
                && Objects.equals(constraints, that.constraints);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, constraints);
    }

    @Override
    public String toString() {
        return "AttributeDefinition{name='" + name + "', type='" + type + "'}";
    }
}
