package com.metaforge.computeengine.interfaces.mcp;

import com.metaforge.computeengine.api.dto.request.AdjacencyQueryRequest;
import com.metaforge.computeengine.api.dto.request.BatchQueryRequest;
import com.metaforge.computeengine.api.dto.request.ClosureQueryRequest;
import com.metaforge.computeengine.api.dto.request.CompositionTreeQueryRequest;
import com.metaforge.computeengine.api.dto.request.CompoundSearchRequest;
import com.metaforge.computeengine.api.dto.request.ImpactDiffusionRequest;
import com.metaforge.computeengine.api.dto.request.MultiHopQueryRequest;
import com.metaforge.computeengine.api.dto.request.PathQueryRequest;
import com.metaforge.computeengine.api.dto.request.PatternMatchRequest;
import com.metaforge.computeengine.api.dto.request.ReachabilityCheckRequest;
import com.metaforge.computeengine.api.dto.request.SubgraphQueryRequest;
import com.metaforge.computeengine.api.dto.response.ClosureResult;
import com.metaforge.computeengine.api.dto.response.GraphQueryResult;
import com.metaforge.computeengine.api.dto.response.ImpactTraceResult;
import com.metaforge.computeengine.api.dto.response.PathResult;
import com.metaforge.computeengine.api.service.GraphQueryService;
import com.metaforge.computeengine.api.service.ImpactTracingService;
import com.metaforge.computeengine.api.service.PathReasoningService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 语义查询引擎 MCP 工具集。
 *
 * <p>通过 Spring AI MCP 暴露图查询操作为 MCP 工具，供 Agent 在语义分析场景下调用。
 *
 * @author metaforge
 */
@Component
public class ComputeEngineMcpTools {

    private final GraphQueryService graphQueryService;
    private final PathReasoningService pathReasoningService;
    private final ImpactTracingService impactTracingService;

    public ComputeEngineMcpTools(GraphQueryService graphQueryService,
                                  PathReasoningService pathReasoningService,
                                  ImpactTracingService impactTracingService) {
        this.graphQueryService = graphQueryService;
        this.pathReasoningService = pathReasoningService;
        this.impactTracingService = impactTracingService;
    }

    @Tool(description = "多度邻接查询：从指定起点实体出发，沿关系边多度扩展，返回每度发现的实体与关系")
    public GraphQueryResult adjacencyQuery(
            @ToolParam(description = "邻接查询请求") AdjacencyQueryRequest request) {
        return graphQueryService.queryAdjacency(request);
    }

    @Tool(description = "组合层级树查询：基于COMPOSITION关系递归展开指定节点的组合结构")
    public GraphQueryResult compositionTree(
            @ToolParam(description = "组合层级树查询请求") CompositionTreeQueryRequest request) {
        return graphQueryService.queryCompositionTree(request);
    }

    @Tool(description = "子图提取查询：从一个或多个中心实体出发提取子图")
    public GraphQueryResult subgraphExtract(
            @ToolParam(description = "子图提取请求") SubgraphQueryRequest request) {
        return graphQueryService.querySubgraph(request);
    }

    @Tool(description = "图模式匹配查询：在线性路径模式中匹配符合模式的路径实例")
    public GraphQueryResult patternMatch(
            @ToolParam(description = "模式匹配请求") PatternMatchRequest request) {
        return graphQueryService.queryPatternMatch(request);
    }

    @Tool(description = "多条件复合检索：按实体类型、属性、关系条件组合过滤检索")
    public List<?> compoundSearch(
            @ToolParam(description = "复合检索请求") CompoundSearchRequest request) {
        return graphQueryService.searchCompound(request).getContent();
    }

    @Tool(description = "批量语义查询：一次传入最多200个FQN，返回实体摘要及关系摘要")
    public GraphQueryResult batchQuery(
            @ToolParam(description = "批量查询请求") BatchQueryRequest request) {
        return graphQueryService.queryBatch(request);
    }

    // ==================== 路径推理工具 ====================

    @Tool(description = "两点间路径查询：查找源实体与目标实体之间的可达路径，支持最短路径优先")
    public PathResult findPaths(
            @ToolParam(description = "路径查询请求") PathQueryRequest request) {
        return pathReasoningService.findPaths(request);
    }

    @Tool(description = "传递闭包推理：基于可传递关系类型计算指定起点的完整闭包")
    public ClosureResult computeClosure(
            @ToolParam(description = "闭包查询请求") ClosureQueryRequest request) {
        return pathReasoningService.computeClosure(request);
    }

    @Tool(description = "多跳语义推理：组合多种关系类型进行跨语义跳跃推理")
    public PathResult multiHopReasoning(
            @ToolParam(description = "多跳推理请求") MultiHopQueryRequest request) {
        return pathReasoningService.multiHopReasoning(request);
    }

    @Tool(description = "路径可达性判定：快速判断源实体到目标实体是否存在可达路径")
    public PathResult checkReachability(
            @ToolParam(description = "可达性判定请求") ReachabilityCheckRequest request) {
        return pathReasoningService.checkReachability(request);
    }

    // ==================== 影响溯源工具 ====================

    @Tool(description = "正向影响扩散：从指定起点沿出边BFS扩散，返回分层影响统计")
    public ImpactTraceResult diffuseForward(
            @ToolParam(description = "影响扩散请求") ImpactDiffusionRequest request) {
        return impactTracingService.diffuseForward(request);
    }

    @Tool(description = "反向依赖溯源：从指定实体沿入边BFS追溯依赖")
    public ImpactTraceResult traceBackward(
            @ToolParam(description = "依赖溯源请求") ImpactDiffusionRequest request) {
        return impactTracingService.traceBackward(request);
    }

    @Tool(description = "影响路径详情查询：查询两实体间的所有影响传导路径")
    public ImpactTraceResult getImpactPaths(
            @ToolParam(description = "源实体FQN") String sourceFqn,
            @ToolParam(description = "目标实体FQN") String targetFqn,
            @ToolParam(description = "关系类型列表") List<String> relationTypes,
            @ToolParam(description = "最大路径深度") int maxDepth) {
        List<com.metaforge.computeengine.api.enums.AssociationType> types =
                relationTypes != null ? relationTypes.stream()
                        .map(com.metaforge.computeengine.api.enums.AssociationType::valueOf)
                        .toList() : null;
        return impactTracingService.getImpactPaths(sourceFqn, targetFqn, types, maxDepth);
    }
}
