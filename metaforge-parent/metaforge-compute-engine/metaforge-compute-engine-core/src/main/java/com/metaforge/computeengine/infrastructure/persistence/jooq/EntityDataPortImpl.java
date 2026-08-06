package com.metaforge.computeengine.infrastructure.persistence.jooq;

import com.metaforge.computeengine.domain.model.valueobject.EntitySnapshot;
import com.metaforge.computeengine.domain.model.valueobject.FQN;
import com.metaforge.computeengine.domain.port.EntityDataPort;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jooq.impl.DSL.*;

/**
 * 实体数据查询端口 jOOQ 适配器实现。
 *
 * <p>通过 jOOQ 跨 Schema 查询 metadata_management.metadata_entity 表（仅 STATUS='ACTIVE' 的生效态数据）。
 *
 * @author metaforge
 */
@Component
public class EntityDataPortImpl implements EntityDataPort {

    private final DSLContext dsl;

    public EntityDataPortImpl(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public EntitySnapshot findByFqn(FQN fqn) {
        Result<Record> result = dsl.select(asterisk())
                .from(table(name("metadata_management", "metadata_entity")))
                .where(field("fqn").eq(fqn.getValue()))
                .limit(1)
                .fetch();

        if (result.isEmpty()) {
            return null;
        }
        return mapToEntitySnapshot(result.get(0), 0);
    }

    @Override
    public List<EntitySnapshot> findByFqnPrefixes(List<String> fqnPrefixes, int limit) {
        if (fqnPrefixes == null || fqnPrefixes.isEmpty()) {
            return Collections.emptyList();
        }

        var condition = noCondition();
        for (String prefix : fqnPrefixes) {
            condition = condition.or(field("fqn", String.class).like(prefix + "%"));
        }

        Result<Record> result = dsl.select(asterisk())
                .from(table(name("metadata_management", "metadata_entity")))
                .where(condition)
                .limit(limit)
                .fetch();

        return mapToEntitySnapshots(result);
    }

    @Override
    public List<EntitySnapshot> findByEntitySchemaFqn(String entitySchemaFqn, int limit) {
        Result<Record> result = dsl.select(asterisk())
                .from(table(name("metadata_management", "metadata_entity")))
                .where(field("entity_schema_fqn").eq(entitySchemaFqn))
                .limit(limit)
                .fetch();

        return mapToEntitySnapshots(result);
    }

    @Override
    public List<EntitySnapshot> batchFindByFqns(List<FQN> fqns) {
        if (fqns == null || fqns.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> fqnValues = fqns.stream().map(FQN::getValue).toList();
        Result<Record> result = dsl.select(asterisk())
                .from(table(name("metadata_management", "metadata_entity")))
                .where(field("fqn", String.class).in(fqnValues))
                .fetch();

        return mapToEntitySnapshots(result);
    }

    private List<EntitySnapshot> mapToEntitySnapshots(Result<Record> records) {
        List<EntitySnapshot> snapshots = new ArrayList<>();
        for (int i = 0; i < records.size(); i++) {
            snapshots.add(mapToEntitySnapshot(records.get(i), 0));
        }
        return snapshots;
    }

    private EntitySnapshot mapToEntitySnapshot(Record record, int depth) {
        String fqnStr = record.get("fqn", String.class);
        String name = record.get("name", String.class);
        String entitySchemaFqnStr = record.get("entity_schema_fqn", String.class);

        Object contentRaw = record.get("content");
        String contentJson;
        if (contentRaw instanceof org.jooq.JSONB jsonb) {
            contentJson = jsonb.data();
        } else if (contentRaw != null) {
            contentJson = String.valueOf(contentRaw);
        } else {
            contentJson = null;
        }

        FQN fqn = new FQN(fqnStr);
        FQN entitySchemaFqn = entitySchemaFqnStr != null ? new FQN(entitySchemaFqnStr) : null;
        Map<String, Object> content = parseContent(contentJson);

        return new EntitySnapshot(fqn, name, entitySchemaFqn, content, depth);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseContent(String contentJson) {
        if (contentJson == null || contentJson.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return com.metaforge.common.util.JsonbUtils.fromJsonb(contentJson, Map.class);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
