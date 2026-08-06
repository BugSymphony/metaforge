package com.metaforge.agent.cognition.core.interfaces.rest;

import com.metaforge.agent.cognition.api.dto.request.CognitionRequest;
import com.metaforge.agent.cognition.api.dto.request.StepGuideRequest;
import com.metaforge.agent.cognition.api.dto.request.SubTaskBriefRequest;
import com.metaforge.agent.cognition.api.dto.response.GuidanceResult;
import com.metaforge.agent.cognition.api.service.CognitionOutputService;
import com.metaforge.agent.cognition.api.service.CognitionQueryService;
import com.metaforge.agent.cognition.core.domain.exception.InvalidScopeModeException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cognition")
@Tag(name = "agent-cognition")
public class CognitionController {

    private static final Logger log = LoggerFactory.getLogger(CognitionController.class);

    private final CognitionQueryService cognitionQueryService;
    private final CognitionOutputService cognitionOutputService;

    public CognitionController(CognitionQueryService cognitionQueryService,
                                CognitionOutputService cognitionOutputService) {
        this.cognitionQueryService = cognitionQueryService;
        this.cognitionOutputService = cognitionOutputService;
    }

    @PostMapping("/{templateId}")
    @Operation(summary = "统一元认知查询引擎", description = "基于模板驱动的多认知视角编排查询，返回结构化认知交付物。支持 json/prompt 双格式输出")
    public Object cognitionQuery(
            @PathVariable String templateId,
            @Valid @RequestBody CognitionRequest request) {
        log.info("认知查询请求: templateId={}, format={}", templateId, request.format());

        GuidanceResult result = cognitionQueryService.execute(templateId, request);

        if ("prompt".equalsIgnoreCase(request.format())) {
            return Map.of(
                    "format", "prompt",
                    "content", cognitionOutputService.formatPrompt(result));
        }
        return result;
    }

    @PostMapping("/step-guide")
    @Operation(summary = "实体即时指导", description = "基于 entity_fqn 的实体级认知查询，自动启用 ENTITY_LEVEL 上下文模式，包含 adjacent_context 局部导航")
    public Object stepGuide(@Valid @RequestBody StepGuideRequest request) {
        log.info("实体即时指导请求: entityFqn={}", request.entityFqn());

        CognitionRequest cognitionRequest = new CognitionRequest(
                List.of(),
                null,
                request.entityFqn(),
                null,
                null,
                null,
                request.cognitionDepth(),
                request.agentArchetype(),
                request.maxTokens(),
                null,
                request.format(),
                null,
                null,
                null);

        GuidanceResult result = cognitionQueryService.execute("step-guide", cognitionRequest);

        if ("prompt".equalsIgnoreCase(request.format())) {
            return Map.of(
                    "format", "prompt",
                    "content", cognitionOutputService.formatPrompt(result));
        }
        return result;
    }

    @PostMapping("/sub-task-brief")
    @Operation(summary = "层级化子任务元认知简报", description = "支持 INHERITED（三层作用域收窄）/ PURE（仅 entity_profile）两种模式，确保子任务上下文隔离")
    public Object subTaskBrief(@Valid @RequestBody SubTaskBriefRequest request) {
        log.info("子任务简报请求: entryEntityFqn={}, scopeMode={}", request.entryEntityFqn(), request.scopeMode());

        String scopeModeValue = request.scopeMode();
        if (!"INHERITED".equalsIgnoreCase(scopeModeValue) && !"PURE".equalsIgnoreCase(scopeModeValue)) {
            throw new InvalidScopeModeException("scope_mode 必须为 INHERITED 或 PURE，实际值: " + scopeModeValue);
        }

        CognitionRequest cognitionRequest = new CognitionRequest(
                List.of(),
                null,
                request.entryEntityFqn(),
                null,
                null,
                request.scopeMode(),
                request.cognitionDepth(),
                request.agentArchetype(),
                request.maxTokens(),
                null,
                request.format(),
                null,
                null,
                null);

        GuidanceResult result = cognitionQueryService.execute("sub-task-brief", cognitionRequest);

        if ("prompt".equalsIgnoreCase(request.format())) {
            return Map.of(
                    "format", "prompt",
                    "content", cognitionOutputService.formatPrompt(result));
        }
        return result;
    }
}
