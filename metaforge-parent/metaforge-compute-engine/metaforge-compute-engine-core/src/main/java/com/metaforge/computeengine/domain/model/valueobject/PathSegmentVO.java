package com.metaforge.computeengine.domain.model.valueobject;

import com.metaforge.computeengine.api.enums.AssociationType;
import com.metaforge.computeengine.api.enums.TraversalDirection;

/**
 * 路径推理中的单个跳步值对象。
 *
 * @author metaforge
 */
public final class PathSegmentVO {

    private final FQN fromEntity;
    private final FQN toEntity;
    private final FQN relation;
    private final AssociationType relationType;
    private final TraversalDirection direction;
    private final double weight;

    public PathSegmentVO(FQN fromEntity, FQN toEntity, FQN relation,
                         AssociationType relationType, TraversalDirection direction, double weight) {
        this.fromEntity = fromEntity;
        this.toEntity = toEntity;
        this.relation = relation;
        this.relationType = relationType;
        this.direction = direction;
        this.weight = weight;
    }

    public FQN getFromEntity() {
        return fromEntity;
    }

    public FQN getToEntity() {
        return toEntity;
    }

    public FQN getRelation() {
        return relation;
    }

    public AssociationType getRelationType() {
        return relationType;
    }

    public TraversalDirection getDirection() {
        return direction;
    }

    public double getWeight() {
        return weight;
    }
}
