package com.metaforge.computeengine.api.dto.response;

import com.metaforge.computeengine.api.enums.AssociationType;
import com.metaforge.computeengine.api.enums.TruncatedReason;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 传递闭包推理结果载体。
 *
 * <p>按可达层级分组展示闭包实体，包含总可达数、途经关系类型统计及截断标记。
 *
 * @param layers            按层级分组的可达实体映射（深度 -> 实体列表）
 * @param totalReachable    可达实体总数
 * @param relationTypeStats 途经关系类型 -> 出现次数统计
 * @param truncated         是否截断
 * @param truncatedReason   截断原因
 * @author metaforge
 */
public record ClosureResult(
        Map<Integer, List<ClosuredEntityDetail>> layers,
        int totalReachable,
        Map<AssociationType, Integer> relationTypeStats,
        boolean truncated,
        TruncatedReason truncatedReason
) implements Serializable {

    /**
     * 闭包中的单个可达实体详情。
     *
     * @param fqn            实体 FQN
     * @param depth          到达的最短深度
     * @param arrivedByTypes 到达该实体途经的关系类型集合
     */
    public record ClosuredEntityDetail(
            String fqn,
            int depth,
            Set<AssociationType> arrivedByTypes
    ) implements Serializable {
    }
}
