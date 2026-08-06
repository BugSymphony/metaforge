package com.metaforge.computeengine.domain.model.valueobject;

import com.metaforge.computeengine.api.enums.AssociationType;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * 关系瞬时快照值对象（只读）。
 *
 * <p>查询过程中获取的关系状态快照，包含源/目标实体 FQN、关系类型、属性内容。
 *
 * @author metaforge
 */
public final class RelationSnapshot {

    private final FQN fqn;
    private final FQN sourceEntityFqn;
    private final FQN targetEntityFqn;
    private final FQN relationSchemaFqn;
    private final AssociationType associationType;
    private final Map<String, Object> content;

    public RelationSnapshot(FQN fqn, FQN sourceEntityFqn, FQN targetEntityFqn,
                            FQN relationSchemaFqn, AssociationType associationType,
                            Map<String, Object> content) {
        this.fqn = Objects.requireNonNull(fqn, "fqn 不能为空");
        this.sourceEntityFqn = sourceEntityFqn;
        this.targetEntityFqn = targetEntityFqn;
        this.relationSchemaFqn = relationSchemaFqn;
        this.associationType = associationType;
        this.content = content != null ? Collections.unmodifiableMap(content) : Collections.emptyMap();
    }

    public FQN getFqn() {
        return fqn;
    }

    public FQN getSourceEntityFqn() {
        return sourceEntityFqn;
    }

    public FQN getTargetEntityFqn() {
        return targetEntityFqn;
    }

    public FQN getRelationSchemaFqn() {
        return relationSchemaFqn;
    }

    public AssociationType getAssociationType() {
        return associationType;
    }

    public Map<String, Object> getContent() {
        return content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RelationSnapshot that)) return false;
        return Objects.equals(fqn, that.fqn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fqn);
    }
}
