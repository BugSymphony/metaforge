package com.metaforge.computeengine.api.dto.request;

import com.metaforge.computeengine.api.dto.common.FilterCriteria;
import com.metaforge.computeengine.api.enums.TraversalDirection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * 组合层级树查询请求。
 *
 * @param rootFqn        根节点实体 FQN
 * @param direction      遍历方向（FORWARD=向下子树，BACKWARD=向上父链，BOTH=双向）
 * @param maxDepth       最大展开深度
 * @param filterCriteria 过滤条件
 * @author metaforge
 */
public record CompositionTreeQueryRequest(
        @NotBlank String rootFqn,
        @NotNull TraversalDirection direction,
        int maxDepth,
        FilterCriteria filterCriteria
) implements Serializable {
}
