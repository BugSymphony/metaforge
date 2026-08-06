package com.metaforge.computeengine.domain.model.valueobject;

import java.util.Objects;

/**
 * Fully Qualified Name（FQN）值对象。
 *
 * <p>封装 FQN 字符串并提供 bundleCode、version、packageFqn、segment 等派生字段。
 * 不可变，通过全属性值相等判断同一性。
 *
 * @author metaforge
 */
public final class FQN {

    private final String value;
    private final String bundleCode;
    private final String version;
    private final String packageFqn;
    private final String segment;

    public FQN(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("FQN value 不能为空");
        }
        this.value = value;

        String[] bundleParts = value.split(":", 2);
        if (bundleParts.length == 2) {
            this.bundleCode = bundleParts[0];
            String rest = bundleParts[1];
            int firstDot = rest.indexOf('.');
            if (firstDot > 0) {
                this.version = rest.substring(0, firstDot);
                String afterVersion = rest.substring(firstDot + 1);
                int lastDot = afterVersion.lastIndexOf('.');
                if (lastDot > 0) {
                    this.packageFqn = afterVersion.substring(0, lastDot);
                    this.segment = afterVersion.substring(lastDot + 1);
                } else {
                    this.packageFqn = "";
                    this.segment = afterVersion;
                }
            } else {
                this.version = rest;
                this.packageFqn = "";
                this.segment = "";
            }
        } else {
            this.bundleCode = "";
            this.version = "";
            this.packageFqn = "";
            this.segment = value;
        }
    }

    public String getValue() {
        return value;
    }

    public String getBundleCode() {
        return bundleCode;
    }

    public String getVersion() {
        return version;
    }

    public String getPackageFqn() {
        return packageFqn;
    }

    public String getSegment() {
        return segment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FQN fqn)) return false;
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
