package com.metaforge.agent.cognition.operator.ontological;

import com.metaforge.agent.cognition.api.enums.DimensionCategory;
import com.metaforge.agent.cognition.api.spi.CognitionQueryContext;
import com.metaforge.agent.cognition.api.spi.CognitionResult;
import com.metaforge.agent.cognition.operator.common.AbstractCognitionOperator;
import com.metaforge.common.dto.PageResult;
import com.metaforge.metamodel.api.dto.ElementQueryRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OntologicalRelationSchemaInventoryOperator extends AbstractCognitionOperator {

    @Override
    public String operatorId() {
        return "ontological.relation-schema-inventory";
    }

    @Override
    public DimensionCategory category() {
        return DimensionCategory.ONTOLOGICAL;
    }

    @Override
    public CognitionResult execute(CognitionQueryContext context) {
        Object query = buildQuery(context);
        Object portResult = executeWithPort(() -> metamodelReadPort.listRelationSchemas(query));
        if (portResult instanceof CognitionResult cr) {
            return cr;
        }

        List<Map<String, Object>> schemas = extractContent(portResult);

        List<Map<String, Object>> lazyNodes = new ArrayList<>();
        for (Map<String, Object> schema : schemas) {
            lazyNodes.add(buildLazyNode(schema, false, null));
        }

        return CognitionResult.success(operatorId(), category(), lazyNodes);
    }

    private ElementQueryRequest buildQuery(CognitionQueryContext context) {
        Map<String, Object> params = context.templateParams();
        if (params != null && params.get("query") instanceof ElementQueryRequest direct) {
            return direct;
        }

        String anchor = null;
        if (params != null) {
            Object bundleFqn = params.get("bundleVersionFqn");
            if (bundleFqn != null) anchor = bundleFqn.toString();
            else {
                Object parentFqn = params.get("parent_fqn");
                if (parentFqn != null) anchor = parentFqn.toString();
            }
        }
        if (anchor == null) {
            List<String> bundleFqns = context.bundleFqns();
            if (bundleFqns != null && !bundleFqns.isEmpty()) {
                anchor = bundleFqns.get(0);
            }
        }

        ElementQueryRequest query = new ElementQueryRequest();
        if (anchor != null && !anchor.isBlank()) {
            query.setFqnPrefixes(List.of(anchor));
        }
        query.setPage(context.cursor() != null && context.cursor() > 0 ? context.cursor() : 1);
        query.setSize(context.pageSize() > 0 ? context.pageSize() : 20);
        return query;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractContent(Object portResult) {
        if (portResult instanceof PageResult<?> pr) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : pr.getContent()) {
                if (item instanceof com.metaforge.metamodel.api.dto.response.RelationSchemaDto dto) {
                    result.add(toRelationSchemaSummary(dto));
                } else if (item instanceof Map) {
                    result.add((Map<String, Object>) item);
                }
            }
            return result;
        }
        return Collections.emptyList();
    }

    /**
     * RelationSchema 精简摘要：仅 fqn/name/description/associationType/enabled。
     */
    private Map<String, Object> toRelationSchemaSummary(
            com.metaforge.metamodel.api.dto.response.RelationSchemaDto dto) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("fqn", dto.getFqn());
        map.put("name", dto.getName());
        map.put("description", dto.getDescription());
        map.put("associationType", dto.getAssociationType());
        map.put("enabled", dto.isEnabled());
        return map;
    }
}
