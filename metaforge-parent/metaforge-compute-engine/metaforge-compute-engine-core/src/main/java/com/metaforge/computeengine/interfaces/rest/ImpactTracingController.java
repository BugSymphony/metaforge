package com.metaforge.computeengine.interfaces.rest;

import com.metaforge.computeengine.api.dto.request.ImpactDiffusionRequest;
import com.metaforge.computeengine.api.dto.request.ImpactPathRequest;
import com.metaforge.computeengine.api.dto.response.ImpactTraceResult;
import com.metaforge.computeengine.api.enums.AssociationType;
import com.metaforge.computeengine.api.service.ImpactTracingService;
import com.metaforge.computeengine.infrastructure.config.ComputeEngineProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 影响溯源 REST Controller。
 *
 * <p>提供三种影响分析能力：正向影响扩散（沿出边 BFS 扩展）、反向依赖溯源（沿入边 BFS 追溯）、
 * 影响路径详情查询（两点间所有传导路径）。
 * <p>正向/反向分析沿指定关系类型 BFS 扩展，按层级分组，同一实体被多路径影响时仅统计一次。
 * 影响路径详情返回两点间所有传导路径，按长度排序，路径内联实体与关系摘要。
 * <p>所有响应由 foundation-core GlobalResponseBodyAdvice 自动包装为 ApiResponse 格式。
 * 错误码范围 33000-33999。
 *
 * @author metaforge
 */
@RestController
@RequestMapping("/api/v1/compute-engine")
@Tag(name = "compute-engine", description = "语义查询与推理引擎 — 多维图遍历检索、路径推理分析、影响溯源评估")
public class ImpactTracingController {

    private final ImpactTracingService impactTracingService;
    private final ComputeEngineProperties properties;

    public ImpactTracingController(ImpactTracingService impactTracingService,
                                    ComputeEngineProperties properties) {
        this.impactTracingService = impactTracingService;
        this.properties = properties;
    }

    @Operation(
            summary = "正向影响扩散",
            description = """
                    从指定起点实体出发，沿指定关系类型沿出边正向 BFS 扩散。\n
                    返回按层级分组的影响实体统计：实体总数、按类型分层统计（entitySchemaFqn -> 数量）、
                    影响实体明细（FQN、影响层级、途经关系类型集合）。\n
                    同一实体被多路径影响时仅统计一次，标注最短影响深度。"""
    )
    @PostMapping("/impact/diffuse")
    public ImpactTraceResult diffuseForward(
            @Parameter(description = """
                    影响扩散请求（中心实体 FQN、扩散方向 FORWARD、最大深度、关注的关系类型集合）""",
                    required = true)
            @Valid @RequestBody ImpactDiffusionRequest request) {
        return impactTracingService.diffuseForward(request);
    }

    @Operation(
            summary = "反向依赖溯源",
            description = """
                    从指定实体出发，沿指定关系类型沿入边反向 BFS 追溯。\n
                    返回所有依赖该实体的上游实体列表，按层级分组展示。\n
                    附带实体与关系内联摘要（FQN、展示名、关联类型、端点 FQN），无需下游额外补查询。"""
    )
    @PostMapping("/impact/trace")
    public ImpactTraceResult traceBackward(
            @Parameter(description = """
                    依赖溯源请求（中心实体 FQN、溯源方向 BACKWARD、最大深度、关注的关系类型集合）""",
                    required = true)
            @Valid @RequestBody ImpactDiffusionRequest request) {
        return impactTracingService.traceBackward(request);
    }

    @Operation(
            summary = "影响路径详情查询",
            description = """
                    查询两指定实体间的所有影响传导路径（不限于单关系类型）。\n
                    按路径长度升序排列，每条路径标注途经实体 FQN、关系 FQN、关系类型（AssociationType）、传导方向。\n
                    路径自包含全部内联摘要——实体摘要 + 关系摘要，无需下游额外查询。"""
    )
    @PostMapping("/impact/paths")
    public ImpactTraceResult getImpactPaths(
            @Parameter(description = "影响路径查询请求（源实体 FQN、目标实体 FQN、关系类型集合）", required = true)
            @Valid @RequestBody ImpactPathRequest request) {
        return impactTracingService.getImpactPaths(
                request.sourceFqn(), request.targetFqn(),
                request.relationTypes() != null
                        ? new java.util.ArrayList<>(request.relationTypes())
                        : null,
                properties.getTraversal().getMaxDepth());
    }

    @Operation(
            summary = "影响路径详情查询（GET）",
            description = """
                    查询两指定实体间的所有影响传导路径（不限于单关系类型），以查询参数方式传参。\n
                    relationTypes 为逗号分隔的 AssociationType 枚举值列表（如 COMPOSITION,DEPENDENCY_INFLUENCE），为空时表示全类型。\n
                    按路径长度升序排列，每条路径标注途经实体 FQN、关系 FQN、关系类型、传导方向。"""
    )
    @GetMapping("/impact/paths")
    public ImpactTraceResult getImpactPathsByParams(
            @Parameter(description = "源实体 FQN", required = true) @RequestParam("sourceFqn") String sourceFqn,
            @Parameter(description = "目标实体 FQN", required = true) @RequestParam("targetFqn") String targetFqn,
            @Parameter(description = "关注的关系类型（逗号分隔），空=全类型") @RequestParam(value = "relationTypes", required = false) String relationTypes,
            @Parameter(description = "最大路径深度，默认取全局配置") @RequestParam(value = "maxDepth", required = false) Integer maxDepth) {
        List<AssociationType> types = parseRelationTypes(relationTypes);
        int depth = maxDepth != null ? maxDepth : properties.getTraversal().getMaxDepth();
        return impactTracingService.getImpactPaths(sourceFqn, targetFqn, types, depth);
    }

    private List<AssociationType> parseRelationTypes(String relationTypes) {
        if (relationTypes == null || relationTypes.isBlank()) return null;
        return java.util.Arrays.stream(relationTypes.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(AssociationType::valueOf)
                .collect(java.util.stream.Collectors.toList());
    }
}
