package com.metaforge.computeengine.domain.model.valueobject;

import com.metaforge.computeengine.api.enums.AssociationType;
import com.metaforge.computeengine.api.enums.TraversalDirection;
import com.metaforge.computeengine.api.enums.WeightStrategy;

/**
 * AssociationType 传导规则配置值对象。
 *
 * <p>定义单个 AssociationType 的传递性、方向、权重策略与深度上限。
 * 来源于 metaforge.compute-engine.transitivity-rules 配置。
 *
 * @author metaforge
 */
public final class TransitivityRule {

    private final AssociationType type;
    private final boolean transitive;
    private final TraversalDirection direction;
    private final WeightStrategy weightStrategy;
    private final int maxDepth;
    private final String description;

    public TransitivityRule(AssociationType type, boolean transitive,
                            TraversalDirection direction, WeightStrategy weightStrategy,
                            int maxDepth, String description) {
        this.type = type;
        this.transitive = transitive;
        this.direction = direction;
        this.weightStrategy = weightStrategy;
        this.maxDepth = maxDepth;
        this.description = description;
    }

    public AssociationType getType() {
        return type;
    }

    public boolean isTransitive() {
        return transitive;
    }

    public TraversalDirection getDirection() {
        return direction;
    }

    public WeightStrategy getWeightStrategy() {
        return weightStrategy;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public String getDescription() {
        return description;
    }
}
