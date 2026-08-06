package com.metaforge.computeengine.domain.model.entity;

import com.metaforge.computeengine.api.enums.AssociationType;
import com.metaforge.computeengine.domain.model.valueobject.FQN;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 传递闭包中的可达实体。
 *
 * <p>记录从起点经可传递关系到达的实体 FQN、最短深度及途经的关系类型集合。
 * 通过 FQN 判断同一性（同一实体多次到达仅保留最短深度记录）。
 *
 * @author metaforge
 */
public class ClosuredEntity {

    private final FQN fqn;
    private final int depth;
    private final Set<AssociationType> arrivedByTypes;

    public ClosuredEntity(FQN fqn, int depth, Set<AssociationType> arrivedByTypes) {
        this.fqn = fqn;
        this.depth = depth;
        this.arrivedByTypes = Collections.unmodifiableSet(
                arrivedByTypes != null ? new LinkedHashSet<>(arrivedByTypes) : new LinkedHashSet<>());
    }

    public FQN getFqn() {
        return fqn;
    }

    public int getDepth() {
        return depth;
    }

    public Set<AssociationType> getArrivedByTypes() {
        return arrivedByTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClosuredEntity that)) return false;
        return Objects.equals(fqn, that.fqn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fqn);
    }
}
