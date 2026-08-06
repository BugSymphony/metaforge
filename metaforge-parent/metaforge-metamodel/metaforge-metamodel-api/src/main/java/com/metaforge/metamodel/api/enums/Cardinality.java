package com.metaforge.metamodel.api.enums;

/**
 * 基数枚举，定义 RelationSchema 源端/目标端多重性。
 */
public enum Cardinality {

    /** 恰好 1 */
    ONE("1"),

    /** 0 或 1 */
    ZERO_OR_ONE("0..1"),

    /** 0 或多个 */
    ZERO_OR_MANY("0..*"),

    /** 1 或多个 */
    ONE_OR_MANY("1..*");

    private final String notation;

    Cardinality(String notation) {
        this.notation = notation;
    }

    public String getNotation() {
        return notation;
    }

    /**
     * 根据记法字符串查找枚举值。
     * <p>兼容平台既有数据的简写记法：
     * <ul>
     *   <li>{@code N} / {@code *} / {@code 0..*} → ZERO_OR_MANY</li>
     *   <li>{@code M} → ONE_OR_MANY</li>
     *   <li>{@code 1} → ONE（原生支持）</li>
     *   <li>{@code 0..1} → ZERO_OR_ONE（原生支持）</li>
     * </ul>
     *
     * @param notation 基数记法（如 "1", "0..1", "0..*", "1..*", "N", "M", "*"）
     * @return 对应的枚举值
     * @throws IllegalArgumentException 如果记法不匹配任何枚举值
     */
    public static Cardinality fromNotation(String notation) {
        if (notation == null) {
            return null;
        }
        String trimmed = notation.trim();
        for (Cardinality c : values()) {
            if (c.notation.equals(trimmed)) {
                return c;
            }
        }
        return switch (trimmed) {
            case "N", "*", "0..*" -> ZERO_OR_MANY;
            case "M", "1..*" -> ONE_OR_MANY;
            default -> throw new IllegalArgumentException("未知的基数记法: " + notation);
        };
    }
}
