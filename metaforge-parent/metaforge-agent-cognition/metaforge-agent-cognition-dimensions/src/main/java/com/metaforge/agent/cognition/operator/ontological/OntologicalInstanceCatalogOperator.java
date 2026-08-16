package com.metaforge.agent.cognition.operator.ontological;

import com.metaforge.agent.cognition.api.enums.DimensionCategory;
import com.metaforge.agent.cognition.api.spi.CognitionQueryContext;
import com.metaforge.agent.cognition.api.spi.CognitionResult;
import com.metaforge.agent.cognition.operator.common.AbstractCognitionOperator;
import com.metaforge.common.dto.PageRequest;
import com.metaforge.common.dto.PageResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OntologicalInstanceCatalogOperator extends AbstractCognitionOperator {

    @Override
    public String operatorId() {
        return "ontological.instance-catalog";
    }

    @Override
    public DimensionCategory category() {
        return DimensionCategory.ONTOLOGICAL;
    }

    @Override
    public CognitionResult execute(CognitionQueryContext context) {
        String entitySchemaFqn = getEntitySchemaFqn(context);
        if (entitySchemaFqn == null || entitySchemaFqn.isBlank()) {
            return wrapFailure("缺少 entitySchemaFqn 参数");
        }

        int page = getPage(context);
        int size = context.pageSize() > 0 ? context.pageSize() : 20;

        Object portResult = executeWithPort(() -> metadataReadPort.listByEntitySchema(entitySchemaFqn, new PageRequest(page, size)));
        if (portResult instanceof CognitionResult cr) {
            return cr;
        }

        List<Map<String, Object>> instances = extractContent(portResult);
        long total = getTotal(portResult);
        int totalPages = calculateTotalPages(total, size);

        Map<String, Object> resultData = new LinkedHashMap<>();
        resultData.put("instances", instances);
        resultData.put("total", total);
        resultData.put("page", page);
        resultData.put("pageSize", size);
        resultData.put("totalPages", totalPages);
        if (page < totalPages) {
            resultData.put("next_cursor", page + 1);
        }

        return CognitionResult.success(operatorId(), category(), resultData);
    }

    private int getPage(CognitionQueryContext context) {
        if (context.cursor() != null && context.cursor() > 0) {
            return context.cursor();
        }
        return 1;
    }

    private String getEntitySchemaFqn(CognitionQueryContext context) {
        Map<String, Object> params = context.templateParams();
        if (params != null && params.containsKey("entitySchemaFqn")) {
            return (String) params.get("entitySchemaFqn");
        }
        List<String> entitySchemas = context.scope() != null ? context.scope().entitySchemas() : null;
        if (entitySchemas != null && !entitySchemas.isEmpty()) {
            return entitySchemas.get(0);
        }
        return null;
    }

    private int calculateTotalPages(long total, int size) {
        if (total <= 0 || size <= 0) return 0;
        return (int) ((total + size - 1) / size);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractContent(Object portResult) {
        if (portResult instanceof PageResult<?> pr) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : pr.getContent()) {
                if (item instanceof com.metaforge.metadata.api.dto.response.MetadataEntityDto dto) {
                    result.add(toContentMap(dto));
                } else if (item instanceof Map) {
                    result.add((Map<String, Object>) item);
                }
            }
            return result;
        }
        return Collections.emptyList();
    }

    private long getTotal(Object portResult) {
        if (portResult instanceof PageResult<?> pr) {
            return pr.getTotal();
        }
        return 0;
    }
}
