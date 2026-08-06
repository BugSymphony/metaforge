package com.metaforge.computeengine.api.dto.response;

import com.metaforge.computeengine.api.enums.AssociationType;
import com.metaforge.computeengine.api.enums.TraversalDirection;
import com.metaforge.computeengine.api.enums.TruncatedReason;

import java.io.Serializable;
import java.util.List;

/**
 * 路径推理结果载体。
 *
 * <p>包含两点间路径列表、路径总数及截断标记。路径按长度排序。
 *
 * @param paths           路径列表（按长度排序）
 * @param totalPaths      路径总数
 * @param truncated       是否截断
 * @param truncatedReason 截断原因
 * @author metaforge
 */
public record PathResult(
        List<PathDetail> paths,
        int totalPaths,
        boolean truncated,
        TruncatedReason truncatedReason
) implements Serializable {

    /**
     * 单条路径详情。
     *
     * @param pathId      路径标识
     * @param steps       路径步骤序列
     * @param totalWeight 路径总权重
     */
    public record PathDetail(
            String pathId,
            List<PathStep> steps,
            double totalWeight
    ) implements Serializable {
    }

    /**
     * 路径中的单个步骤。
     *
     * @param fromEntityFqn 起始实体 FQN
     * @param toEntityFqn   终止实体 FQN
     * @param relationFqn   关系实例 FQN
     * @param relationType  关系类型
     * @param direction     遍历方向
     * @param weight        该步权重
     */
    public record PathStep(
            String fromEntityFqn,
            String toEntityFqn,
            String relationFqn,
            AssociationType relationType,
            TraversalDirection direction,
            double weight
    ) implements Serializable {
    }
}
