package com.metaforge.computeengine.domain.model.entity;

import com.metaforge.computeengine.domain.model.valueobject.FQN;
import com.metaforge.computeengine.domain.model.valueobject.PathSegmentVO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 遍历路径领域实体。
 *
 * <p>表示图遍历过程中追踪的一条路径，包含路径段序列与总权重。
 *
 * @author metaforge
 */
public class TraversalPath {

    private final String pathId;
    private final List<PathSegmentVO> segments;
    private double totalWeight;

    public TraversalPath() {
        this.pathId = UUID.randomUUID().toString();
        this.segments = new ArrayList<>();
        this.totalWeight = 0.0;
    }

    public String getPathId() {
        return pathId;
    }

    public List<PathSegmentVO> getSegments() {
        return Collections.unmodifiableList(segments);
    }

    public double getTotalWeight() {
        return totalWeight;
    }

    /**
     * 添加路径段并累加权重。
     */
    public void addSegment(PathSegmentVO segment) {
        segments.add(segment);
        totalWeight += segment.getWeight();
    }

    /**
     * 判断路径中是否包含指定实体。
     */
    public boolean containsEntity(FQN entityFqn) {
        for (PathSegmentVO segment : segments) {
            if (entityFqn.equals(segment.getFromEntity()) || entityFqn.equals(segment.getToEntity())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取路径的最后一个实体。
     */
    public FQN getLastEntity() {
        if (segments.isEmpty()) return null;
        return segments.get(segments.size() - 1).getToEntity();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TraversalPath that)) return false;
        return Objects.equals(pathId, that.pathId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pathId);
    }
}
