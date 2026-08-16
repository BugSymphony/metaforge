package com.metaforge.agent.cognition.operator.ontological;

import com.metaforge.agent.cognition.api.enums.DimensionCategory;
import com.metaforge.agent.cognition.api.spi.CognitionQueryContext;
import com.metaforge.agent.cognition.api.spi.CognitionResult;
import com.metaforge.agent.cognition.operator.common.AbstractCognitionOperator;
import com.metaforge.graph.api.dto.RelationInstanceDto;
import com.metaforge.metadata.api.dto.response.MetadataEntityDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OntologicalEntityProfileOperator extends AbstractCognitionOperator {

    @Override
    public String operatorId() {
        return "ontological.entity-profile";
    }

    @Override
    public DimensionCategory category() {
        return DimensionCategory.ONTOLOGICAL;
    }

    @Override
    public CognitionResult execute(CognitionQueryContext context) {
        String entityFqn = context.entityFqn();
        if (entityFqn == null || entityFqn.isBlank()) {
            return wrapFailure("缺少 entityFqn 参数");
        }

        Object entityResult = executeWithPort(() -> metadataReadPort.getByFqn(entityFqn));
        if (entityResult instanceof CognitionResult cr) {
            return cr;
        }
        if (!(entityResult instanceof MetadataEntityDto entity)) {
            return wrapFailure("实体不存在或返回结构异常: " + entityFqn);
        }

        String entitySchemaFqn = resolveEntitySchemaFqn(context, entity);
        Map<String, Object> entitySchema = null;
        if (entitySchemaFqn != null) {
            Object schemaResult = executeWithPort(() -> metamodelReadPort.getEntitySchema(entitySchemaFqn));
            if (schemaResult instanceof Map<?, ?> m) {
                @SuppressWarnings("unchecked")
                Map<String, Object> es = (Map<String, Object>) m;
                entitySchema = es;
            }
        }

        List<String> domainLocation = resolveDomainLocation(entityFqn);

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("entity", toContentMap(entity));
        if (entitySchema != null) {
            profile.put("entitySchema", entitySchema);
        }
        profile.put("domain_location", domainLocation);

        return CognitionResult.success(operatorId(), category(), profile);
    }

    private String resolveEntitySchemaFqn(CognitionQueryContext context, MetadataEntityDto entity) {
        Map<String, Object> params = context.templateParams();
        if (params != null && params.get("entitySchemaFqn") != null) {
            return params.get("entitySchemaFqn").toString();
        }
        if (entity.getEntitySchemaFqn() != null && !entity.getEntitySchemaFqn().isBlank()) {
            return entity.getEntitySchemaFqn();
        }
        List<String> schemas = context.scope() != null ? context.scope().entitySchemas() : null;
        if (schemas != null && !schemas.isEmpty()) {
            return schemas.get(0);
        }
        return null;
    }

    private List<String> resolveDomainLocation(String entityFqn) {
        List<String> path = new ArrayList<>();
        String current = entityFqn;

        for (int i = 0; i < 10; i++) {
            final String target = current;
            Object result = executeWithPort(() -> graphReadPort.getInboundRelations(target, "COMPOSITION", null));
            if (result instanceof CognitionResult) break;

            if (result instanceof List<?> list && !list.isEmpty()) {
                Object first = list.get(0);
                if (!(first instanceof RelationInstanceDto edge)) break;
                String parent = edge.getSourceEntityFqn();
                if (parent != null && !parent.equals(current)) {
                    path.add(0, parent);
                    current = parent;
                } else {
                    break;
                }
            } else {
                break;
            }
        }

        path.add(entityFqn);
        return path;
    }
}
