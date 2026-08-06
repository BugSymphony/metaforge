package com.metaforge.metadata.domain.model.valueobject;

import java.util.Objects;

/**
 * 不可变版本号值对象，从 1 开始递增。
 */
public final class VersionNumber implements Comparable<VersionNumber> {
    private final int value;

    private VersionNumber(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("版本号不能小于 1");
        }
        this.value = value;
    }

    public static VersionNumber of(int value) {
        return new VersionNumber(value);
    }

    public static VersionNumber initial() {
        return new VersionNumber(1);
    }

    public VersionNumber increment() {
        return new VersionNumber(value + 1);
    }

    public int getValue() { return value; }

    @Override
    public int compareTo(VersionNumber other) {
        return Integer.compare(this.value, other.value);
    }

    @Override
    public String toString() { return String.valueOf(value); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VersionNumber that)) return false;
        return value == that.value;
    }

    @Override
    public int hashCode() { return Integer.hashCode(value); }
}
