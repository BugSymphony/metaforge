package com.metaforge.metadata.domain.model.valueobject;

import java.util.Objects;

/**
 * M2 层元模型 EntitySchema 的全限定名值对象，携带完整版本号。
 * 格式：bundleCode ":" version "." packagePath "." schemaName
 * 示例：order:1.0.0.pkg_order.Order
 */
public final class EntitySchemaFQN {
    private final String value;

    private EntitySchemaFQN(String value) {
        this.value = Objects.requireNonNull(value);
    }

    public static EntitySchemaFQN of(String value) {
        return new EntitySchemaFQN(value);
    }

    public String getValue() { return value; }

    public String getBundleCode() {
        int idx = value.indexOf(':');
        return idx >= 0 ? value.substring(0, idx) : value;
    }

    public String getVersion() {
        int start = value.indexOf(':');
        int end = value.indexOf('.');
        if (start >= 0 && end > start) {
            return value.substring(start + 1, end);
        }
        return "";
    }

    @Override
    public String toString() { return value; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntitySchemaFQN that)) return false;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() { return value.hashCode(); }
}
