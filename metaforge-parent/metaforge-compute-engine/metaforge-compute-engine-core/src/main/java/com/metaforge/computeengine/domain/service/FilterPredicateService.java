package com.metaforge.computeengine.domain.service;

import com.metaforge.computeengine.api.dto.common.FilterCriteria;
import com.metaforge.computeengine.api.enums.MatchMode;
import com.metaforge.computeengine.domain.model.valueobject.FilterCriteriaVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 过滤谓词构建领域服务。
 *
 * <p>将 FilterCriteriaVO 中的 7 维过滤条件转换为 jOOQ SQL Condition 列表。
 * 维度间 AND 逻辑，维度内 OR 逻辑。matchMode 决定匹配策略：
 * PREFIX → LIKE 'prefix%'，EXACT → =，PATTERN → LIKE。
 *
 * @author metaforge
 */
@Service
public class FilterPredicateService {

    private static final Logger log = LoggerFactory.getLogger(FilterPredicateService.class);

    /**
     * 构建过滤条件表达式列表。
     *
     * @param filter 过滤条件值对象
     * @return SQL 条件字符串列表（用于 CTE WHERE 子句）
     */
    public List<String> buildFilterConditions(FilterCriteriaVO filter) {
        if (filter == null || filter.isEmpty()) {
            return List.of();
        }
        return List.of(); // CTE WHERE 条件在具体查询中按需构建
    }

    /**
     * 构建实体类型过滤 SQL 条件。
     */
    public String buildEntityTypeCondition(List<FilterCriteria.FqnFilterGroup> entityTypes) {
        if (entityTypes == null || entityTypes.isEmpty()) return null;

        return entityTypes.stream()
                .map(g -> buildFqnCondition("entity_schema_fqn", g.value(), g.matchMode()))
                .collect(Collectors.joining(" OR ", "(", ")"));
    }

    /**
     * 构建关系类型过滤 SQL 条件。
     */
    public String buildRelationTypeCondition(List<FilterCriteria.FqnFilterGroup> relationTypes) {
        if (relationTypes == null || relationTypes.isEmpty()) return null;

        return relationTypes.stream()
                .map(g -> buildFqnCondition("relation_schema_fqn", g.value(), g.matchMode()))
                .collect(Collectors.joining(" OR ", "(", ")"));
    }

    /**
     * 构建 FQN 前缀匹配 SQL 条件。
     */
    public String buildFqnPrefixCondition(String column, List<String> prefixes) {
        if (prefixes == null || prefixes.isEmpty()) return null;

        return prefixes.stream()
                .map(p -> column + " LIKE '" + p + "%'")
                .collect(Collectors.joining(" OR ", "(", ")"));
    }

    /**
     * 构建属性字段 JSONB 等值匹配条件。
     */
    public String buildPropertyCondition(List<FilterCriteria.PropertyFilter> propertyFilters) {
        if (propertyFilters == null || propertyFilters.isEmpty()) return null;

        return propertyFilters.stream()
                .map(pf -> buildJsonbCondition(pf.field(), pf.value()))
                .collect(Collectors.joining(" AND "));
    }

    private String buildFqnCondition(String column, String value, MatchMode mode) {
        return switch (mode) {
            case EXACT -> column + " = '" + value + "'";
            case PREFIX -> column + " LIKE '" + value + "%'";
            case PATTERN -> column + " LIKE '" + value + "'";
        };
    }

    private String buildJsonbCondition(String field, String value) {
        return "content @> '{\"" + field + "\": \"" + value + "\"}'::jsonb";
    }
}
