package com.metaforge.computeengine.api.dto.request;

import com.metaforge.computeengine.api.dto.common.FilterCriteria;
import com.metaforge.computeengine.api.enums.AssociationType;
import com.metaforge.computeengine.api.enums.TraversalDirection;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.List;

/**
 * 多跳语义推理请求。
 *
 * @param sourceFqn      起点实体 FQN
 * @param hopSteps       跃步序列（上限 5 步，对齐 compute-engine traversal.max-depth）
 * @param filterCriteria 过滤条件
 * @author metaforge
 */
public record MultiHopQueryRequest(
        @NotBlank String sourceFqn,
        @NotEmpty @Size(max = 5) List<@Valid HopStep> hopSteps,
        FilterCriteria filterCriteria
) implements Serializable {

    /**
     * 单个跃步定义。
     *
     * @param relationType 关系类型
     * @param direction    遍历方向
     */
    public record HopStep(
            @NotNull AssociationType relationType,
            @NotNull TraversalDirection direction
    ) implements Serializable {
    }
}
