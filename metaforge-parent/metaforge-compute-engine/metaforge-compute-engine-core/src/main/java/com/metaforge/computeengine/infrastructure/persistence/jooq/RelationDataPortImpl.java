package com.metaforge.computeengine.infrastructure.persistence.jooq;

import com.metaforge.computeengine.api.enums.AssociationType;
import com.metaforge.computeengine.api.enums.TraversalDirection;
import com.metaforge.computeengine.domain.model.valueobject.FQN;
import com.metaforge.computeengine.domain.model.valueobject.RelationSnapshot;
import com.metaforge.computeengine.domain.port.RelationDataPort;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jooq.impl.DSL.*;

/**
 * 关系数据查询端口 jOOQ 适配器实现。
 *
 * <p>通过 jOOQ 跨 Schema 查询 semantic_relation_network.relation_instance 与
 * semantic_relation_network.entity_relation_index 表（仅 STATUS='ACTIVE' 的生效态数据）。
 *
 * @author metaforge
 */
@Component
public class RelationDataPortImpl implements RelationDataPort {

    private final DSLContext dsl;

    public RelationDataPortImpl(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public List<RelationSnapshot> findOutboundRelations(FQN entityFqn, Set<AssociationType> types, int limit) {
        return findRelationsByDirection(entityFqn, "OUTBOUND", types, limit);
    }

    @Override
    public List<RelationSnapshot> findInboundRelations(FQN entityFqn, Set<AssociationType> types, int limit) {
        return findRelationsByDirection(entityFqn, "INBOUND", types, limit);
    }

    @Override
    public List<RelationSnapshot> findRelations(FQN entityFqn, TraversalDirection direction,
                                                 Set<AssociationType> types, int limit) {
        return switch (direction) {
            case FORWARD -> findOutboundRelations(entityFqn, types, limit);
            case BACKWARD -> findInboundRelations(entityFqn, types, limit);
            case BIDIRECTIONAL, DIRECTED -> {
                List<RelationSnapshot> all = new ArrayList<>();
                all.addAll(findOutboundRelations(entityFqn, types, limit / 2));
                all.addAll(findInboundRelations(entityFqn, types, limit / 2));
                yield all;
            }
        };
    }

    @Override
    public RelationSnapshot findByFqn(FQN relationFqn) {
        Result<Record> result = dsl.select(asterisk())
                .from(table(name("semantic_relation_network", "relation_instance")))
                .where(field("fqn").eq(relationFqn.getValue()))
                .limit(1)
                .fetch();

        if (result.isEmpty()) {
            return null;
        }
        return mapToRelationSnapshot(result.get(0));
    }

    private List<RelationSnapshot> findRelationsByDirection(FQN entityFqn, String directionStr,
                                                              Set<AssociationType> types, int limit) {
        var query = dsl.select(asterisk())
                .from(table(name("semantic_relation_network", "entity_relation_index")).as("eri"))
                .join(table(name("semantic_relation_network", "relation_instance")).as("ri"))
                .on(field("eri.relation_fqn").eq(field("ri.fqn")))
                .where(field("eri.entity_fqn").eq(entityFqn.getValue()))
                .and(field("eri.direction").eq(directionStr));

        if (types != null && !types.isEmpty()) {
            List<String> typeNames = types.stream().map(Enum::name).toList();
            query = query.and(field("ri.relation_type", String.class).in(typeNames));
        }

        Result<Record> result = query.limit(limit).fetch();
        return mapToRelationSnapshots(result);
    }

    private List<RelationSnapshot> mapToRelationSnapshots(Result<Record> records) {
        List<RelationSnapshot> snapshots = new ArrayList<>();
        for (int i = 0; i < records.size(); i++) {
            snapshots.add(mapToRelationSnapshot(records.get(i)));
        }
        return snapshots;
    }

    private RelationSnapshot mapToRelationSnapshot(Record record) {
        String fqnStr = record.get("fqn", String.class);
        String sourceFqnStr = record.get("source_entity_fqn", String.class);
        String targetFqnStr = record.get("target_entity_fqn", String.class);
        String relationSchemaFqnStr = record.get("relation_schema_fqn", String.class);
        String associationTypeStr = record.get("relation_type", String.class);
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
        FQN sourceFqn = sourceFqnStr != null ? new FQN(sourceFqnStr) : null;
        FQN targetFqn = targetFqnStr != null ? new FQN(targetFqnStr) : null;
        FQN relationSchemaFqn = relationSchemaFqnStr != null ? new FQN(relationSchemaFqnStr) : null;
        AssociationType associationType = parseAssociationType(associationTypeStr);

        return new RelationSnapshot(fqn, sourceFqn, targetFqn, relationSchemaFqn,
                associationType, parseContent(contentJson));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseContent(String contentJson) {
        if (contentJson == null || contentJson.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Object> content = com.metaforge.common.util.JsonbUtils.fromJsonb(contentJson, Map.class);
            return content != null ? content : Collections.emptyMap();
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private AssociationType parseAssociationType(String value) {
        if (value == null) return null;
        try {
            return AssociationType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
