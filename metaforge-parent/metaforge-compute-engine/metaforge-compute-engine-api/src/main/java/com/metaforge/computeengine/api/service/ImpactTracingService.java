package com.metaforge.computeengine.api.service;

import com.metaforge.computeengine.api.annotation.OpenHostService;
import com.metaforge.computeengine.api.enums.AssociationType;
import com.metaforge.computeengine.api.dto.request.ImpactDiffusionRequest;
import com.metaforge.computeengine.api.dto.response.ImpactTraceResult;

import java.util.List;

/**
 * 影响溯源与变更评估服务。
 * <p>
 * 提供三种影响分析能力：正向影响扩散（沿出边 BFS 扩展）、反向依赖溯源（沿入边 BFS 追溯）、
 * 影响路径详情查询（两点间所有传导路径）。
 * <p>
 * 正向/反向分析沿指定关系类型 BFS 扩展，按层级分组，同一实体被多路径影响时仅统计一次。
 * 影响路径详情返回两点间所有传导路径，按长度排序，路径内联实体与关系摘要（无需下游额外补查询）。
 */
@OpenHostService
public interface ImpactTracingService {

    /**
     * 正向影响扩散查询。
     * <p>
     * 从指定起点实体出发，沿指定关系类型沿出边正向 BFS 扩散。
     * 返回按层级分组的影响实体统计（实体总数、按类型分层统计、影响实体明细含 FQN/类型/层级/深度）。
     * 同一实体被多路径影响时仅统计一次，标注最短影响深度。
     *
     * @param request 影响扩散请求（起点 FQN、关系类型列表、最大深度）
     * @return 影响溯源结果（影响实体总数、分层统计、实体明细、关系明细、截断标记）
     */
    ImpactTraceResult diffuseForward(ImpactDiffusionRequest request);

    /**
     * 反向依赖溯源查询。
     * <p>
     * 从指定实体出发，沿指定关系类型沿入边反向 BFS 追溯。
     * 返回所有依赖该实体的上游实体列表，按层级分组展示，附带实体与关系内联摘要。
     *
     * @param request 依赖溯源请求（起点 FQN、关系类型列表、最大深度）
     * @return 影响溯源结果（依赖实体总数、分层统计、实体明细、关系明细、截断标记）
     */
    ImpactTraceResult traceBackward(ImpactDiffusionRequest request);

    /**
     * 影响路径详情查询。
     * <p>
     * 查询两指定实体间的所有影响传导路径（不限于单关系类型）。
     * 按路径长度升序排列，每条路径标注途经实体 FQN、关系 FQN、关系类型、传导方向。
     * 路径自包含全部内联摘要，无需下游额外查询。
     *
     * @param sourceFqn     源实体 FQN
     * @param targetFqn     目标实体 FQN
     * @param relationTypes 关注的关系类型列表（空=全类型）
     * @param maxDepth      最大路径深度
     * @return 影响溯源结果（路径列表、路径总数）
     */
    ImpactTraceResult getImpactPaths(String sourceFqn, String targetFqn,
                                     List<AssociationType> relationTypes, int maxDepth);
}
