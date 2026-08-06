package com.metaforge.computeengine.api.dto.request;

import com.metaforge.computeengine.api.enums.AssociationType;
import com.metaforge.computeengine.api.enums.TraversalDirection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.Set;

/**
 * 影响扩散查询请求。
 *
 * @param centerFqn     中心实体 FQN
 * @param direction     扩散方向（FORWARD=正向影响，BACKWARD=反向依赖）
 * @param maxDepth      最大扩散深度
 * @param relationTypes 关注的关系类型集合（空=全类型）
 * @author metaforge
 */
public record ImpactDiffusionRequest(
        @NotBlank String centerFqn,
        @NotNull TraversalDirection direction,
        int maxDepth,
        Set<AssociationType> relationTypes
) implements Serializable {
}
