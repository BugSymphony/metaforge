package com.metaforge.computeengine.domain.model.entity;

import com.metaforge.computeengine.api.enums.AssociationType;
import com.metaforge.computeengine.domain.model.valueobject.FQN;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 影响溯源中的受影响实体。
 *
 * <p>记录影响传播中的实体 FQN、影响深度、影响传导途经的关系类型集合。
 * 同一实体被多路径影响时仅统计一次（通过 FQN 判断同一性）。
 *
 * @author metaforge
 */
public class ImpactEntity {

    private final FQN fqn;
    private final int depth;
    private final Set<AssociationType> affectedByTypes;

    public ImpactEntity(FQN fqn, int depth, Set<AssociationType> affectedByTypes) {
        this.fqn = fqn;
        this.depth = depth;
        this.affectedByTypes = Collections.unmodifiableSet(
                affectedByTypes != null ? new LinkedHashSet<>(affectedByTypes) : new LinkedHashSet<>());
    }

    public FQN getFqn() { return fqn; }
    public int getDepth() { return depth; }
    public Set<AssociationType> getAffectedByTypes() { return affectedByTypes; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImpactEntity that)) return false;
        return Objects.equals(fqn, that.fqn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fqn);
    }
}
