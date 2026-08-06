package com.metaforge.metadata.domain.model.valueobject;

import java.util.Objects;

/**
 * M1 层元数据实例的全限定名值对象。
 * 格式：segment["." segment]*，segment 文法 [A-Za-z][A-Za-z0-9_-]*。
 * 创建后不可变更。
 */
public final class FQN {
    private final String value;

    private FQN(String value) {
        this.value = Objects.requireNonNull(value, "FQN 不能为 null");
    }

    public static FQN of(String value) {
        return new FQN(value);
    }

    public String getValue() { return value; }

    @Override
    public String toString() { return value; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FQN fqn)) return false;
        return value.equals(fqn.value);
    }

    @Override
    public int hashCode() { return value.hashCode(); }
}
