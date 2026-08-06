package com.metaforge.computeengine.api.dto.request;

import com.metaforge.computeengine.api.enums.AssociationType;
import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;
import java.util.Set;

/**
 * 影响路径详情查询请求。
 *
 * @param sourceFqn     源实体 FQN
 * @param targetFqn     目标实体 FQN
 * @param relationTypes 关注的关系类型集合（空=全类型）
 * @author metaforge
 */
public record ImpactPathRequest(
        @NotBlank String sourceFqn,
        @NotBlank String targetFqn,
        Set<AssociationType> relationTypes
) implements Serializable {
}
