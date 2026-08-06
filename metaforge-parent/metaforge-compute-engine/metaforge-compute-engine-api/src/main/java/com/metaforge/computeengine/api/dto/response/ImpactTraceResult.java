package com.metaforge.computeengine.api.dto.response;

import com.metaforge.computeengine.api.dto.common.RelationSummary;
import com.metaforge.computeengine.api.enums.AssociationType;
import com.metaforge.computeengine.api.enums.TruncatedReason;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 影响溯源结果载体。
 *
 * <p>包含按层级分组的影响实体、类型统计、关联关系明细及截断标记。
 *
 * @param totalImpacted   影响实体总数
 * @param layerStats      按层级分组的影响实体映射（深度 -> 实体列表）
 * @param typeStats       按实体类型 FQN 分层的统计数
 * @param entities        影响实体明细（去重）
 * @param relations       关联关系明细（去重）
 * @param truncated       是否截断
 * @param truncatedReason 截断原因
 * @author metaforge
 */
public record ImpactTraceResult(
        int totalImpacted,
        Map<Integer, List<ImpactEntityDetail>> layerStats,
        Map<String, Integer> typeStats,
        List<ImpactEntityDetail> entities,
        List<RelationSummary> relations,
        boolean truncated,
        TruncatedReason truncatedReason
) implements Serializable {

    /**
     * 影响溯源中的受影响实体详情。
     *
     * @param fqn             实体 FQN
     * @param depth           影响传播层级
     * @param affectedByTypes 影响传导途经的关系类型集合
     */
    public record ImpactEntityDetail(
            String fqn,
            int depth,
            Set<AssociationType> affectedByTypes
    ) implements Serializable {
    }
}
