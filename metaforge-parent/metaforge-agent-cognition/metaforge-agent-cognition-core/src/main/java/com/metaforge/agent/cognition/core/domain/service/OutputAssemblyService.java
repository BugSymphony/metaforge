package com.metaforge.agent.cognition.core.domain.service;

import com.metaforge.agent.cognition.api.dto.response.CognitionResponse;
import com.metaforge.agent.cognition.api.dto.response.ContextMeta;
import com.metaforge.agent.cognition.api.enums.OutputFormat;
import com.metaforge.agent.cognition.api.spi.CognitionResult;
import com.metaforge.agent.cognition.core.domain.model.aggregate.CognitionQuery;
import com.metaforge.agent.cognition.core.domain.model.valueobject.OperatorId;
import com.metaforge.agent.cognition.core.domain.model.valueobject.ScopeBehavior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class OutputAssemblyService {

    private static final Logger log = LoggerFactory.getLogger(OutputAssemblyService.class);

    private static final String UPDATED_SCOPE_KEY = "updated_scope";

    private final ContextMetaService contextMetaService;

    public OutputAssemblyService(ContextMetaService contextMetaService) {
        this.contextMetaService = contextMetaService;
    }

    public CognitionResponse assemble(CognitionQuery query) {
        // 先提取 updated_scope（可能使部分算子 data 变空），再收集扁平算子列表并过滤空数据
        Map<String, Object> updatedScope = resolveUpdatedScope(query);

        List<CognitionResult> dimensions = collectResults(query);

        ContextMeta contextMeta = contextMetaService.generate(
                query.getTemplateId().value(),
                query.getScope(),
                query.getTokenBudget().estimated(),
                query.getSkippedEntities(),
                query.getTruncatedPerspectives()
        );

        if (query.getOutputFormat() == OutputFormat.JSON) {
            return CognitionResponse.json(query.getTemplateId().value(), contextMeta, dimensions, updatedScope);
        } else {
            return CognitionResponse.prompt(query.getTemplateId().value(), contextMeta, dimensions, updatedScope);
        }
    }

    /**
     * 按算子编排顺序收集结果（扁平列表，保留 category 字段），过滤 data 为空的算子；
     * 并注入模板算子配置的 name（模板 YAML operators[].name）。
     */
    private List<CognitionResult> collectResults(CognitionQuery query) {
        List<CognitionResult> results = new ArrayList<>();
        Map<OperatorId, CognitionResult> executionResults = query.getExecutionResults();
        if (executionResults == null) {
            return results;
        }
        Map<String, com.metaforge.agent.cognition.core.domain.model.entity.OperatorDefinition> opDefs =
                resolveOperators(query.getTemplateDefinition());
        for (CognitionResult result : executionResults.values()) {
            if (result == null || isEmptyData(result.data())) {
                continue;
            }
            var opDef = opDefs.get(result.operatorId());
            String name = opDef != null ? opDef.getName() : null;
            String description = opDef != null ? opDef.getDescription() : null;
            if (name != null || description != null) {
                results.add(CognitionResult.successWithMeta(
                        result.operatorId(), name, result.category(), description, result.data()));
            } else {
                results.add(result);
            }
        }
        return results;
    }

    private Map<String, com.metaforge.agent.cognition.core.domain.model.entity.OperatorDefinition> resolveOperators(
            com.metaforge.agent.cognition.core.domain.model.entity.TemplateDefinition def) {
        Map<String, com.metaforge.agent.cognition.core.domain.model.entity.OperatorDefinition> map = new LinkedHashMap<>();
        if (def != null && def.getOperators() != null) {
            for (com.metaforge.agent.cognition.core.domain.model.entity.OperatorDefinition op : def.getOperators()) {
                if (op.getOperatorId() != null) {
                    map.put(op.getOperatorId(), op);
                }
            }
        }
        return map;
    }

    private boolean isEmptyData(Object data) {
        if (data == null) {
            return true;
        }
        if (data instanceof java.util.Collection<?> c) {
            return c.isEmpty();
        }
        if (data instanceof Map<?, ?> m) {
            return m.isEmpty();
        }
        return false;
    }

    /**
     * 从各算子 data 中提取 updated_scope 并移除（不暴露在 dimensions 内）；
     * 仅当模板声明 producesUpdatedScope=true 时聚合到响应顶层，否则返回 null
     * （Jackson non_null 过滤，响应不出现 updatedScope 字段）。
     */
    private Map<String, Object> resolveUpdatedScope(CognitionQuery query) {
        ScopeBehavior sb = query.getTemplateDefinition() != null
                ? query.getTemplateDefinition().getScopeBehavior() : null;
        boolean produces = sb != null && sb.isProducesUpdatedScope();

        Map<String, Object> merged = produces ? new LinkedHashMap<>() : null;
        for (CognitionResult result : query.getExecutionResults().values()) {
            if (result == null || !result.success() || !(result.data() instanceof Map<?, ?> raw)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) raw;
            Object updated = data.remove(UPDATED_SCOPE_KEY);
            if (produces && updated instanceof Map<?, ?> us) {
                merged.putAll((Map<String, Object>) us);
            }
        }
        return merged;
    }
}
