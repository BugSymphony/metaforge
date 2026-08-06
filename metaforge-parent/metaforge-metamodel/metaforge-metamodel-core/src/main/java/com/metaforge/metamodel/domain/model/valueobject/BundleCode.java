package com.metaforge.metamodel.domain.model.valueobject;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Bundle Code 值对象，正则校验 [a-z][a-z0-9_-]{2,63}。
 * 禁止包含 ':' 和 '.' 字符。
 */
public final class BundleCode {

    private static final Pattern PATTERN = Pattern.compile("[a-z][a-z0-9_-]{2,63}");

    private final String value;

    private BundleCode(String value) {
        this.value = value;
    }

    /**
     * 创建 BundleCode 实例，校验格式合规性。
     *
     * @param code 候选 code 值
     * @return BundleCode 实例
     * @throws IllegalArgumentException 如果格式不合法
     */
    public static BundleCode of(String code) {
        if (code == null || !PATTERN.matcher(code).matches()) {
            throw new IllegalArgumentException(
                    "Bundle code 格式不合法: " + code + "，需匹配正则 [a-z][a-z0-9_-]{2,63}");
        }
        return new BundleCode(code);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BundleCode that)) return false;
        return Objects.equals(value, that.value);
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
