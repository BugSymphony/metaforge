package com.metaforge.computeengine.interfaces.rest;

import com.metaforge.computeengine.api.dto.request.ClosureQueryRequest;
import com.metaforge.computeengine.api.dto.request.MultiHopQueryRequest;
import com.metaforge.computeengine.api.dto.request.PathQueryRequest;
import com.metaforge.computeengine.api.dto.request.ReachabilityCheckRequest;
import com.metaforge.computeengine.api.dto.response.ClosureResult;
import com.metaforge.computeengine.api.dto.response.PathResult;
import com.metaforge.computeengine.api.service.PathReasoningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 路径推理 REST Controller。
 *
 * <p>提供四种推理能力：两点间路径查询（含最短路径）、传递闭包推理、多跳语义推理、路径可达性判定。
 * <p>所有推理规则基于 {@code metaforge.compute-engine.transitivity-rules} 配置中定义的
 * AssociationType 传递性、方向与传导矩阵执行。传递路径的遍历深度受各 AssociationType per-type maxDepth 约束。
 * 多跳语义推理中相邻跳的关系类型须满足传导兼容性。
 * <p>所有响应由 foundation-core GlobalResponseBodyAdvice 自动包装为 ApiResponse 格式。
 * 错误码范围 33000-33999。
 *
 * @author metaforge
 */
@RestController
@RequestMapping("/api/v1/compute-engine")
@Tag(name = "compute-engine", description = "语义查询与推理引擎 — 多维图遍历检索、路径推理分析、影响溯源评估")
public class PathReasoningController {

    private final PathReasoningService pathReasoningService;

    public PathReasoningController(PathReasoningService pathReasoningService) {
        this.pathReasoningService = pathReasoningService;
    }

    @Operation(
            summary = "两点间路径查询",
            description = """
                    查询指定源实体与目标实体之间的可达路径。\n
                    支持指定遍历方向（FORWARD/BACKWARD/DIRECTED/BIDIRECTIONAL）、关系类型过滤、最大深度约束。\n
                    可选择返回全部路径或仅返回最短路径（按边数最少）。\n
                    路径结果按长度或权重排序，内联每个跳步的实体与关系摘要。"""
    )
    @PostMapping("/paths")
    public PathResult findPaths(
            @Parameter(description = """
                    路径查询请求（源/目标 FQN、遍历方向、关系类型集合、最大深度、是否仅返回最短路径、过滤条件）""",
                    required = true)
            @Valid @RequestBody PathQueryRequest request) {
        return pathReasoningService.findPaths(request);
    }

    @Operation(
            summary = "传递闭包推理",
            description = """
                    基于配置中定义的可传递关系类型（transitive=true），计算指定起点沿传递关系的完整闭包。\n
                    结果按传递层级分层分组（depth -> 闭包实体列表），每层包含该深度发现的可达实体。\n
                    途经关系类型统计展示各 AssociationType 在闭包中的出现次数。\n
                    循环引用自动去重截断，遇 non-transitive 关系类型或深度超限时该分支截断，
                    但不影响其他可传递类型边在深度范围内的继续遍历。"""
    )
    @PostMapping("/closure")
    public ClosureResult computeClosure(
            @Parameter(description = "闭包推理请求（起点 FQN、关系类型过滤、过滤条件）", required = true)
            @Valid @RequestBody ClosureQueryRequest request) {
        return pathReasoningService.computeClosure(request);
    }

    @Operation(
            summary = "多跳语义推理",
            description = """
                    基于传导规则配置中的传导矩阵，组合多种关系类型进行跨语义跳跃推理。\n
                    每步跳跃的关系类型须满足配置中定义的传导兼容性。\n
                    权重策略（MULTIPLY/ADD/MAX/NONE）用于计算路径的置信度或成本。\n
                    最大跳跃步数 3 步。跃步序列定义每步的关系类型（如 COMPOSITION）与遍历方向（如 FORWARD）。"""
    )
    @PostMapping("/multi-hop")
    public PathResult multiHopReasoning(
            @Parameter(description = "多跳推理请求（起点 FQN、跃步序列 max 3 步、过滤条件）", required = true)
            @Valid @RequestBody MultiHopQueryRequest request) {
        return pathReasoningService.multiHopReasoning(request);
    }

    @Operation(
            summary = "路径可达性判定",
            description = """
                    快速判定源实体到目标实体是否存在可达路径。\n
                    在找到任意一条可达路径后立即返回结果（LIMIT 1 早期终止），不继续搜索更多路径。\n
                    性能优先于完整性——返回首条路径后即截断。"""
    )
    @PostMapping("/reachability")
    public PathResult checkReachability(
            @Parameter(description = "可达性判定请求（源/目标 FQN、关系类型集合）", required = true)
            @Valid @RequestBody ReachabilityCheckRequest request) {
        return pathReasoningService.checkReachability(request);
    }
}
