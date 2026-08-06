package com.metaforge.computeengine.infrastructure.persistence.jooq.converter;

import com.metaforge.common.util.JsonbUtils;
import com.metaforge.computeengine.api.enums.AssociationType;
import com.metaforge.computeengine.domain.model.valueobject.FQN;
import com.metaforge.computeengine.domain.model.valueobject.RelationSnapshot;
import org.jooq.Record;

import java.util.Collections;
import java.util.Map;

/**
 * jOOQ Record -> RelationSnapshot 转换器。
 *
 * <p>将 jOOQ 查询结果 Record 转换为领域值对象 RelationSnapshot，包括 JSONB content 字段的反序列化。
 *
 * @author metaforge
 */
public final class RelationConverter {

    private RelationConverter() {
    }

    /**
     * 将 jOOQ Record 转换为 RelationSnapshot。
     *
     * @param record jOOQ 查询结果行
     * @return RelationSnapshot 实例
     */
    public static RelationSnapshot toRelationSnapshot(Record record) {
        String fqnStr = record.get("fqn", String.class);
        String sourceFqnStr = record.get("source_entity_fqn", String.class);
        String targetFqnStr = record.get("target_entity_fqn", String.class);
        String relationSchemaFqnStr = record.get("relation_schema_fqn", String.class);
        String associationTypeStr = record.get("relation_type", String.class);
        String contentJson = record.get("content", String.class);

        FQN fqn = new FQN(fqnStr);
        FQN sourceFqn = sourceFqnStr != null ? new FQN(sourceFqnStr) : null;
        FQN targetFqn = targetFqnStr != null ? new FQN(targetFqnStr) : null;
        FQN relationSchemaFqn = relationSchemaFqnStr != null ? new FQN(relationSchemaFqnStr) : null;
        AssociationType associationType = parseAssociationType(associationTypeStr);
        Map<String, Object> content = parseContent(contentJson);

        return new RelationSnapshot(fqn, sourceFqn, targetFqn, relationSchemaFqn,
                associationType, content);
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

    private static AssociationType parseAssociationType(String value) {
        if (value == null) return null;
        try {
            return AssociationType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
