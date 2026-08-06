package com.metaforge.computeengine.api.dto.request;

import com.metaforge.computeengine.api.dto.common.FilterCriteria;
import com.metaforge.computeengine.api.enums.AssociationType;
import com.metaforge.computeengine.api.enums.TraversalDirection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.Set;

/**
 * 邻接查询请求。
 *
 * @param sourceFqn      起点实体 FQN
 * @param direction      遍历方向
 * @param maxDepth       最大遍历深度（1-10）
 * @param relationTypes  关注的关系类型集合（空=全类型）
 * @param filterCriteria 7 维过滤条件
 * @author metaforge
 */
public record AdjacencyQueryRequest(
        @NotBlank String sourceFqn,
        @NotNull TraversalDirection direction,
        int maxDepth,
        Set<AssociationType> relationTypes,
        FilterCriteria filterCriteria
) implements Serializable {
}
