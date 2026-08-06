package com.metaforge.graph.domain.model.valueobject;

import java.util.Objects;

/**
 * CardinalityRule 值对象——关系基数约束（源端:目标端）。
 */
public final class CardinalityRule {

    private final String sourceCardinality;
    private final String targetCardinality;

    private CardinalityRule(String sourceCardinality, String targetCardinality) {
        this.sourceCardinality = sourceCardinality;
        this.targetCardinality = targetCardinality;
    }

    public static CardinalityRule of(String sourceCardinality, String targetCardinality) {
        if (sourceCardinality == null || sourceCardinality.isBlank()) {
            throw new IllegalArgumentException("源端基数不能为空");
        }
        if (targetCardinality == null || targetCardinality.isBlank()) {
            throw new IllegalArgumentException("目标端基数不能为空");
        }
        return new CardinalityRule(sourceCardinality, targetCardinality);
    }

    public String getSourceCardinality() {
        return sourceCardinality;
    }

    public String getTargetCardinality() {
        return targetCardinality;
    }

    public boolean isManyToOne() {
        return "N".equals(sourceCardinality) && "1".equals(targetCardinality);
    }

    public boolean isOneToOne() {
        return "1".equals(sourceCardinality) && "1".equals(targetCardinality);
    }

    public boolean isManyToMany() {
        return "N".equals(sourceCardinality) && "N".equals(targetCardinality);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CardinalityRule that)) return false;
        return Objects.equals(sourceCardinality, that.sourceCardinality)
                && Objects.equals(targetCardinality, that.targetCardinality);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceCardinality, targetCardinality);
    }

    @Override
    public String toString() {
        return sourceCardinality + ":" + targetCardinality;
    }
}
