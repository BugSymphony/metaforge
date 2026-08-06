package com.metaforge.computeengine.api.dto.request;

import com.metaforge.computeengine.api.enums.AssociationType;
import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;
import java.util.Set;

/**
 * 路径可达性判定请求。
 *
 * @param sourceFqn     起点实体 FQN
 * @param targetFqn     终点实体 FQN
 * @param relationTypes 关系类型过滤
 * @author metaforge
 */
public record ReachabilityCheckRequest(
        @NotBlank String sourceFqn,
        @NotBlank String targetFqn,
        Set<AssociationType> relationTypes
) implements Serializable {
}
