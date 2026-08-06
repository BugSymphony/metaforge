package com.metaforge.computeengine.api.dto.common;

import com.metaforge.computeengine.api.enums.AssociationType;
import com.metaforge.computeengine.api.enums.MatchMode;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 7 维过滤条件 DTO。
 *
 * <p>维度间 AND 逻辑，维度内集合 OR 逻辑。所有维度均为可选。被过滤内容不参与遍历且不计入深度。
 *
 * @param associationTypes     关联类型过滤集合
 * @param sourceFqns           源实体 FQN 过滤列表
 * @param targetFqns           目标实体 FQN 过滤列表
 * @param relationInstanceFqns 关系实例 FQN 过滤列表（支持 PATTERN 模式）
 * @param entityTypes          实体类型（EntitySchema FQN）过滤列表
 * @param relationTypes        关系类型（RelationSchema FQN）过滤列表
 * @param propertyFilters      属性字段精确等值匹配列表
 * @param logicOperator        维度内逻辑运算符（AND/OR），默认 AND
 * @author metaforge
 */
public record FilterCriteria(
        Set<AssociationType> associationTypes,
        List<FqnFilterGroup> sourceFqns,
        List<FqnFilterGroup> targetFqns,
        List<FqnFilterGroup> relationInstanceFqns,
        List<FqnFilterGroup> entityTypes,
        List<FqnFilterGroup> relationTypes,
        List<PropertyFilter> propertyFilters,
        LogicOperator logicOperator
) implements Serializable {

    public FilterCriteria {
        if (associationTypes == null) associationTypes = Collections.emptySet();
        if (sourceFqns == null) sourceFqns = Collections.emptyList();
        if (targetFqns == null) targetFqns = Collections.emptyList();
        if (relationInstanceFqns == null) relationInstanceFqns = Collections.emptyList();
        if (entityTypes == null) entityTypes = Collections.emptyList();
        if (relationTypes == null) relationTypes = Collections.emptyList();
        if (propertyFilters == null) propertyFilters = Collections.emptyList();
        if (logicOperator == null) logicOperator = LogicOperator.AND;
    }

    /**
     * FQN 过滤组。
     *
     * @param value     FQN/前缀/模式字符串
     * @param matchMode 匹配模式
     */
    public record FqnFilterGroup(
            String value,
            MatchMode matchMode
    ) implements Serializable {
    }

    /**
     * 属性字段精确等值匹配条件。
     *
     * @param field 属性字段名（JSONB key）
     * @param value 属性字段值（精确匹配）
     */
    public record PropertyFilter(
            String field,
            String value
    ) implements Serializable {
    }

    /**
     * 逻辑运算符（维度内）。
     */
    public enum LogicOperator {
        AND,
        OR
    }
}
