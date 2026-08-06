package com.metaforge.metamodel.domain.model.valueobject;

import java.util.Objects;
import java.util.Set;

/**
 * 原生属性定义值对象，对应 EntitySchema/RelationSchema 的 native_attributes 项。
 * 类型限定为 string/number/integer/boolean/array 五类，不支持 object 嵌套。
 */
public final class NativeAttribute {

    private static final Set<String> VALID_TYPES =
            Set.of("string", "number", "integer", "boolean", "array");

    private final String name;
    private final String type;
    private final boolean required;
    private final String defaultValue;
    private final String description;

    private NativeAttribute(String name, String type, boolean required,
                            String defaultValue, String description) {
        this.name = name;
        this.type = type;
        this.required = required;
        this.defaultValue = defaultValue;
        this.description = description;
    }

    public static NativeAttribute of(String name, String type, boolean required,
                                      String defaultValue, String description) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("属性名不能为空");
        }
        if (type == null || !VALID_TYPES.contains(type)) {
            throw new IllegalArgumentException(
                    "属性类型不合法: " + type + "，仅支持 string/number/integer/boolean/array");
        }
        return new NativeAttribute(name, type, required, defaultValue, description);
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public boolean isRequired() {
        return required;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NativeAttribute that)) return false;
        return required == that.required
                && Objects.equals(name, that.name)
                && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, required);
    }

    @Override
    public String toString() {
        return "NativeAttribute{name='" + name + "', type='" + type + "'}";
    }
}
