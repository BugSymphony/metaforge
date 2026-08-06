package com.metaforge.computeengine.api.dto.response;

import com.metaforge.computeengine.api.dto.common.EntitySummary;
import com.metaforge.computeengine.api.dto.common.RelationSummary;
import com.metaforge.computeengine.api.enums.TruncatedReason;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 图查询结果载体。
 *
 * <p>统一承载多维图查询（邻接、组合层级树、子图提取、模式匹配、批量查询）的返回结果。
 * 所有实体与关系去重，邻接映射按实体 FQN 组织。统一包含截断标记。
 *
 * @param entities         实体集合（去重，含内联摘要）
 * @param relations        关系集合（去重）
 * @param adjacencyMap     实体 FQN -> 关联关系 FQN 列表的邻接映射
 * @param truncated        是否截断
 * @param truncatedReason  截断原因
 * @param notFoundFqns     未能找到的 FQN 列表（仅批量查询使用）
 * @author metaforge
 */
public record GraphQueryResult(
        List<EntitySummary> entities,
        List<RelationSummary> relations,
        Map<String, List<String>> adjacencyMap,
        boolean truncated,
        TruncatedReason truncatedReason,
        List<String> notFoundFqns
) implements Serializable {
}
