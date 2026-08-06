package com.metaforge.computeengine.infrastructure.persistence.jooq.converter;

import com.metaforge.common.util.JsonbUtils;
import com.metaforge.computeengine.domain.model.valueobject.EntitySnapshot;
import com.metaforge.computeengine.domain.model.valueobject.FQN;
import org.jooq.Record;

import java.util.Collections;
import java.util.Map;

/**
 * jOOQ Record -> EntitySnapshot 转换器。
 *
 * <p>将 jOOQ 查询结果 Record 转换为领域值对象 EntitySnapshot，包括 JSONB content 字段的反序列化。
 *
 * @author metaforge
 */
public final class EntityConverter {

    private EntityConverter() {
    }

    /**
     * 将 jOOQ Record 转换为 EntitySnapshot。
     *
     * @param record jOOQ 查询结果行
     * @param depth  到达该实体的深度
     * @return EntitySnapshot 实例
     */
    public static EntitySnapshot toEntitySnapshot(Record record, int depth) {
        String fqnStr = record.get("fqn", String.class);
        String name = record.get("name", String.class);
        String entitySchemaFqnStr = record.get("entity_schema_fqn", String.class);
        String contentJson = record.get("content", String.class);

        FQN fqn = new FQN(fqnStr);
        FQN entitySchemaFqn = entitySchemaFqnStr != null ? new FQN(entitySchemaFqnStr) : null;
        Map<String, Object> content = parseContent(contentJson);

        return new EntitySnapshot(fqn, name, entitySchemaFqn, content, depth);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseContent(String contentJson) {
        if (contentJson == null || contentJson.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return JsonbUtils.fromJsonb(contentJson, Map.class);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
