package com.metaforge.computeengine.api.dto.request;

import com.metaforge.computeengine.api.dto.common.FilterCriteria;
import jakarta.validation.constraints.NotEmpty;

import java.io.Serializable;
import java.util.List;

/**
 * 子图提取查询请求。
 *
 * @param centerFqns     中心实体 FQN 列表（至少 1 个）
 * @param maxDepth        扩展深度（1-3）
 * @param filterCriteria  过滤条件
 * @author metaforge
 */
public record SubgraphQueryRequest(
        @NotEmpty List<String> centerFqns,
        int maxDepth,
        FilterCriteria filterCriteria
) implements Serializable {
}
