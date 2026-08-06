package com.metaforge.computeengine.domain.model.valueobject;

import com.metaforge.computeengine.api.enums.AssociationType;
import com.metaforge.computeengine.api.enums.TraversalDirection;

import java.util.Collections;
import java.util.List;

/**
 * 影响溯源范围描述值对象。
 *
 * <p>定义影响分析的中心实体、方向、深度及关注的关系类型范围。
 *
 * @author metaforge
 */
public final class InfluenceScope {

    private final FQN centerFqn;
    private final TraversalDirection direction;
    private final int maxDepth;
    private final List<AssociationType> relationTypes;

    public InfluenceScope(FQN centerFqn, TraversalDirection direction,
                          int maxDepth, List<AssociationType> relationTypes) {
        this.centerFqn = centerFqn;
        this.direction = direction;
        this.maxDepth = maxDepth;
        this.relationTypes = relationTypes != null
                ? Collections.unmodifiableList(relationTypes) : Collections.emptyList();
    }

    public FQN getCenterFqn() {
        return centerFqn;
    }

    public TraversalDirection getDirection() {
        return direction;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public List<AssociationType> getRelationTypes() {
        return relationTypes;
    }
}
