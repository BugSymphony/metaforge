package com.metaforge.graph.domain.model.valueobject;

import java.util.Objects;

/**
 * VersionNumber 值对象——关系实例的版本号（≥1，单调递增）。
 */
public final class VersionNumber implements Comparable<VersionNumber> {

    private final int value;

    private VersionNumber(int value) {
        this.value = value;
    }

    public static VersionNumber of(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("版本号必须 ≥1，实际值: " + value);
        }
        return new VersionNumber(value);
    }

    public static VersionNumber initial() {
        return new VersionNumber(1);
    }

    public int getValue() {
        return value;
    }

    public VersionNumber next() {
        return new VersionNumber(value + 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VersionNumber that)) return false;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public int compareTo(VersionNumber other) {
        return Integer.compare(this.value, other.value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
