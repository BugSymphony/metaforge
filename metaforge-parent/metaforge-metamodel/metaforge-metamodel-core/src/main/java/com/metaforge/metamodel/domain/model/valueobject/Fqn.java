package com.metaforge.metamodel.domain.model.valueobject;

import com.metaforge.metamodel.api.enums.EntityType;

import java.util.Objects;

/**
 * FQN 值对象，封装纯净 FQN 字符串并提供类型推导与派生字段。
 * 不进行格式校验（校验由上层写入/发布环节完成）。
 */
public final class Fqn {

    private final String value;

    private Fqn(String value) {
        this.value = value;
    }

    public static Fqn of(String value) {
        return new Fqn(value);
    }

    public String getValue() {
        return value;
    }

    /**
     * 推导实体类型：Bundle 无 ':'，BundleVersion 有 ':' 无 '.' 的 segment，
     * 其余根据层级判断。
     */
    public EntityType detectType() {
        if (!value.contains(":")) {
            return EntityType.BUNDLE;
        }
        if (!value.contains(".")) {
            return EntityType.BUNDLE_VERSION;
        }
        return EntityType.ENTITY_SCHEMA;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Fqn fqn)) return false;
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
