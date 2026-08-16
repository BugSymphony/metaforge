package com.metaforge.agent.cognition.core.interfaces.mcp;

import com.metaforge.agent.cognition.api.dto.request.CognitionRequest;
import com.metaforge.agent.cognition.api.dto.request.Scope;
import com.metaforge.agent.cognition.api.dto.response.CognitionResponse;
import com.metaforge.agent.cognition.api.enums.AgentArchetype;
import com.metaforge.agent.cognition.api.enums.CognitionDepth;
import com.metaforge.agent.cognition.api.enums.OutputFormat;
import com.metaforge.agent.cognition.api.service.CognitionQueryService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CognitionMcpTools {

    private final CognitionQueryService cognitionQueryService;

    public CognitionMcpTools(CognitionQueryService cognitionQueryService) {
        this.cognitionQueryService = cognitionQueryService;
    }

    @Tool(name = "cognition_execute", description = "执行认知查询：按模板ID编排认知算子，产出结构化认知简报。结果可直接注入LLM上下文——低理解成本、自包含、带完整来源标注。模板ID可选: DISCOVER(元模型发现), ORIENT(业务域定位), BRIEF(实体全景), GUIDE(执行指南), FORECAST(影响链路), DELEGATE(子任务委派)。")
    public CognitionResponse executeCognition(
            @ToolParam(description = "模板ID (DISCOVER/ORIENT/BRIEF/GUIDE/FORECAST/DELEGATE)") String templateId,
            @ToolParam(description = "认知边界五字段。scope中bundles白名单即授权依据") Scope scope,
            @ToolParam(description = "模板专用参数，见各模板inputSchema") Map<String, Object> params,
            @ToolParam(description = "输出格式 (json/prompt)，默认json") String format,
            @ToolParam(description = "认知深度 (L1概览/L2标准/L3全量)，默认L2") String cognitionDepth,
            @ToolParam(description = "Agent原型 (execution/exploration/audit/orchestration)") String agentArchetype,
            @ToolParam(description = "最大Token预算，默认8000；<500自动降L1") Integer maxTokens) {

        OutputFormat outputFormat = format != null && !format.isBlank()
                ? OutputFormat.valueOf(format.trim().toUpperCase(java.util.Locale.ROOT)) : null;
        CognitionDepth depth = cognitionDepth != null && !cognitionDepth.isBlank()
                ? CognitionDepth.valueOf(cognitionDepth.trim().toUpperCase(java.util.Locale.ROOT)) : null;
        AgentArchetype archetype = agentArchetype != null && !agentArchetype.isBlank()
                ? AgentArchetype.valueOf(agentArchetype.trim().toUpperCase(java.util.Locale.ROOT)) : null;

        CognitionRequest request = new CognitionRequest(scope, params,
                outputFormat != null ? outputFormat.name() : null, depth, archetype, maxTokens);
        return cognitionQueryService.execute(templateId, request);
    }
}
