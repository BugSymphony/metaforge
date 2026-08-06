package com.metaforge.computeengine.domain.model.valueobject;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * 实体瞬时快照值对象（只读）。
 *
 * <p>查询过程中获取的实体状态快照，包含 FQN、展示名、元模型类型 FQN、属性内容及到达深度。
 *
 * @author metaforge
 */
public final class EntitySnapshot {

    private final FQN fqn;
    private final String name;
    private final FQN entitySchemaFqn;
    private final Map<String, Object> content;
    private final int depth;

    public EntitySnapshot(FQN fqn, String name, FQN entitySchemaFqn,
                          Map<String, Object> content, int depth) {
        this.fqn = Objects.requireNonNull(fqn, "fqn 不能为空");
        this.name = name;
        this.entitySchemaFqn = entitySchemaFqn;
        this.content = content != null ? Collections.unmodifiableMap(content) : Collections.emptyMap();
        this.depth = depth;
    }

    public FQN getFqn() {
        return fqn;
    }

    public String getName() {
        return name;
    }

    public FQN getEntitySchemaFqn() {
        return entitySchemaFqn;
    }

    public Map<String, Object> getContent() {
        return content;
    }

    public int getDepth() {
        return depth;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntitySnapshot that)) return false;
        return Objects.equals(fqn, that.fqn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fqn);
    }
}
