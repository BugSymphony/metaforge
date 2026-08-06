package com.metaforge.computeengine.api.dto.request;

import com.metaforge.computeengine.api.dto.common.FilterCriteria;
import com.metaforge.computeengine.api.enums.AssociationType;
import com.metaforge.computeengine.api.enums.TraversalDirection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.Set;

/**
 * 路径查询请求。
 *
 * @param sourceFqn      起点实体 FQN
 * @param targetFqn      终点实体 FQN
 * @param direction      遍历方向
 * @param maxDepth       最大遍历深度
 * @param relationTypes  关系类型过滤
 * @param findShortest   是否仅返回最短路径
 * @param filterCriteria 过滤条件
 * @author metaforge
 */
public record PathQueryRequest(
        @NotBlank String sourceFqn,
        @NotBlank String targetFqn,
        @NotNull TraversalDirection direction,
        int maxDepth,
        Set<AssociationType> relationTypes,
        boolean findShortest,
        FilterCriteria filterCriteria
) implements Serializable {
}
