package com.metaforge.computeengine.api.dto.request;

import com.metaforge.computeengine.api.dto.common.FilterCriteria;
import com.metaforge.computeengine.api.enums.AssociationType;
import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;
import java.util.Set;

/**
 * 传递闭包查询请求。
 *
 * @param sourceFqn      起点实体 FQN
 * @param relationTypes  关系类型过滤（空=全类型，仅传递类型生效）
 * @param filterCriteria 过滤条件
 * @author metaforge
 */
public record ClosureQueryRequest(
        @NotBlank String sourceFqn,
        Set<AssociationType> relationTypes,
        FilterCriteria filterCriteria
) implements Serializable {
}
