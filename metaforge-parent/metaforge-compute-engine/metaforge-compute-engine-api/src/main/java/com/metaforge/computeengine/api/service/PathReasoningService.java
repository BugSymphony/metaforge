package com.metaforge.computeengine.api.service;

import com.metaforge.computeengine.api.annotation.OpenHostService;
import com.metaforge.computeengine.api.dto.request.ClosureQueryRequest;
import com.metaforge.computeengine.api.dto.request.MultiHopQueryRequest;
import com.metaforge.computeengine.api.dto.request.PathQueryRequest;
import com.metaforge.computeengine.api.dto.request.ReachabilityCheckRequest;
import com.metaforge.computeengine.api.dto.response.ClosureResult;
import com.metaforge.computeengine.api.dto.response.PathResult;

/**
 * 路径推理与语义关联分析服务。
 * <p>
 * 提供四种推理能力：两点间路径查询（支持最短路径优先）、传递闭包推理、
 * 多跳语义推理（跨语义类型跳跃）、路径可达性快速判定。
 * <p>
 * 所有推理规则基于 {@code metaforge.compute-engine.transitivity-rules} 配置中定义的
 * AssociationType 传递性、方向与传导矩阵执行。实体/关系结构来源于已发布元模型定义。
 * 传递路径的遍历深度受各 AssociationType per-type maxDepth 约束。
 * 多跳语义推理中相邻跳的关系类型须满足配置中定义的传导兼容性。
 */
@OpenHostService
public interface PathReasoningService {

    /**
     * 两点间路径查询。
     * <p>
     * 查询指定源实体与目标实体之间的可达路径，支持指定遍历方向、关系类型过滤、最大深度约束。
     * 可选择返回全部路径或仅返回最短路径（按边数最少）。
     *
     * @param request 路径查询请求（源/目标 FQN、遍历方向、关系类型、最大深度、最短路径模式）
     * @return 路径推理结果（路径列表、路径总数、截断标记）
     */
    PathResult findPaths(PathQueryRequest request);

    /**
     * 传递闭包推理。
     * <p>
     * 基于配置中定义的可传递关系类型（transitive=true），计算指定起点沿传递关系的完整闭包。
     * 结果按传递层级分层分组，每层包含该深度发现的可达实体。
     * 循环引用自动去重截断，遇 non-transitive 关系类型或深度超限时该分支截断，
     * 但不影响其他可传递类型边在深度范围内的继续遍历。
     *
     * @param request 闭包推理请求（起点 FQN、关系类型过滤）
     * @return 传递闭包结果（按层级分组的可达实体、可达总数、关系类型统计、截断标记）
     */
    ClosureResult computeClosure(ClosureQueryRequest request);

    /**
     * 多跳语义推理。
     * <p>
     * 基于传导规则配置中的传导矩阵，组合多种关系类型进行跨语义跳跃推理。
     * 每步跳跃的关系类型须满足配置中定义的传导兼容性。
     * 权重策略（multiply/add/max）用于计算路径的置信度或成本。
     * 最大跳跃步数 3 步。
     *
     * @param request 多跳推理请求（起点 FQN、跳步序列（每步的关系类型+方向））
     * @return 路径推理结果（推理路径、每步语义说明、截断标记）
     */
    PathResult multiHopReasoning(MultiHopQueryRequest request);

    /**
     * 路径可达性快速判定。
     * <p>
     * 快速判定源实体到目标实体是否存在可达路径。
     * 在找到任意一条可达路径后立即返回结果，不继续搜索更多路径。
     * 性能优先于完整性，使用 LIMIT 1 截断搜索。
     *
     * @param request 可达性判定请求（源/目标 FQN、关系类型、最大深度）
     * @return 可达性结果（可达标志、最短深度、首条路径）
     */
    PathResult checkReachability(ReachabilityCheckRequest request);
}
