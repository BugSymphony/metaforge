package com.metaforge.agent.cognition.core.interfaces.mcp;

import com.metaforge.agent.cognition.api.dto.request.CognitionRequest;
import com.metaforge.agent.cognition.api.dto.response.GuidanceResult;
import com.metaforge.agent.cognition.api.service.CognitionQueryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 元认知指导层 BC 的 MCP 工具集。
 *
 * <p>通过 Spring AI {@code @Tool} 注解将领域能力发布为 MCP Server 工具方法，
 * 供 {@code agent-consumption} BC 经 {@code spring-ai-starter-mcp-server-webmvc}
 * 自动注册到 MCP Server 并暴露给 Agent 消费端调用。
 *
 * <p><strong>工具集名称</strong>：{@code agent-cognition}<br>
 * <strong>适用场景</strong>：Agent 获取结构化元认知上下文，支持 6 种内置模板，14 个认知视角维度。
 */
@Component
public class CognitionMcpTools {

    private static final Logger log = LoggerFactory.getLogger(CognitionMcpTools.class);

    private final CognitionQueryService cognitionQueryService;

    public CognitionMcpTools(CognitionQueryService cognitionQueryService) {
        this.cognitionQueryService = cognitionQueryService;
    }

    @Tool(description = "执行元认知查询，基于模板驱动的多认知视角编排，返回结构化认知交付物。支持 6 种内置模板（task-brief/step-guide/bundle-catalog/navigate/cognition-guidance/sub-task-brief）和 14 个认知视角维度")
    public GuidanceResult cognitionExecute(
            @ToolParam(description = "模板 ID，取值：task-brief、step-guide、bundle-catalog、navigate、cognition-guidance、sub-task-brief") String templateId,
            @ToolParam(description = "Bundle FQN 列表，如 [\"order:1.0.0\", \"refund:1.0.0\"]") List<String> bundleFqns,
            @ToolParam(description = "实体 FQN（可选），传入则启用 ENTITY_LEVEL 实体级上下文模式") String entityFqn,
            @ToolParam(description = "认知深度：L1（最多 3 视角）、L2（最多 7 视角，默认）、L3（全部 14 视角）") String cognitionDepth,
            @ToolParam(description = "代理原型：execution（执行型）/ exploration（探索型）/ audit（审计型）/ orchestration（编排型），默认 execution") String agentArchetype,
            @ToolParam(description = "Token 预算上限，默认 8000") Integer maxTokens,
            @ToolParam(description = "输出格式：json（结构化 JSON）或 prompt（Markdown，可直接注入 LLM 上下文），默认 json") String format) {

        log.info("MCP 认知查询: templateId={}, bundleFqns={}, entityFqn={}, depth={}, archetype={}",
                templateId, bundleFqns, entityFqn, cognitionDepth, agentArchetype);

        CognitionRequest request = new CognitionRequest(
                bundleFqns,
                null,
                entityFqn,
                null,
                null,
                null,
                cognitionDepth != null ? cognitionDepth : "L2",
                agentArchetype != null ? agentArchetype : "execution",
                maxTokens != null ? maxTokens : 8000,
                null,
                format != null ? format : "json",
                null,
                null,
                null);

        return cognitionQueryService.execute(templateId, request);
    }
}
