package com.metaforge.metadata.domain.model.valueobject;

import java.util.Map;
import java.util.Objects;

/**
 * 上游 EntitySchema 编译产出的 JSON Schema 快照缓存值对象。
 * 与 entitySchemaFqn 一一对应，传递给 JSON Schema Validator 执行运行时校验。
 */
public final class JsonSchemaSnapshot {
    private final String entitySchemaFqn;
    private final Map<String, Object> schema;

    public JsonSchemaSnapshot(String entitySchemaFqn, Map<String, Object> schema) {
        this.entitySchemaFqn = Objects.requireNonNull(entitySchemaFqn);
        this.schema = Objects.requireNonNull(schema);
    }

    public String getEntitySchemaFqn() { return entitySchemaFqn; }
    public Map<String, Object> getSchema() { return schema; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JsonSchemaSnapshot that)) return false;
        return entitySchemaFqn.equals(that.entitySchemaFqn);
    }

    @Override
    public int hashCode() { return entitySchemaFqn.hashCode(); }
}
