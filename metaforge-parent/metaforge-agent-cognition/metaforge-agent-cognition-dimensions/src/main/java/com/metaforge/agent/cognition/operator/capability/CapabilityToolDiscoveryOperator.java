package com.metaforge.agent.cognition.operator.capability;

import com.metaforge.agent.cognition.api.enums.DimensionCategory;
import com.metaforge.agent.cognition.api.spi.CognitionQueryContext;
import com.metaforge.agent.cognition.api.spi.CognitionResult;
import com.metaforge.agent.cognition.operator.common.AbstractCognitionOperator;
import com.metaforge.agent.cognition.operator.common.MetaforgeLibraryFqns;
import com.metaforge.graph.api.dto.RelationInstanceDto;
import com.metaforge.metadata.api.dto.response.MetadataEntityDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 能力清单（工具发现）——按关系 schema 精确绑定「使用方 → Capability」三类关系，
 * 返回分层摘要（fqn/name/description/protocolType），不含 interface_spec 细节。
 */
@Component
public class CapabilityToolDiscoveryOperator extends AbstractCognitionOperator {

    private static final List<String> DEFAULT_CAPABILITY_RELATIONS = List.of(
            MetaforgeLibraryFqns.Relation.AGENT_HAS_CAPABILITY,
            MetaforgeLibraryFqns.Relation.TASK_REQUIRES_CAPABILITY,
            MetaforgeLibraryFqns.Relation.STEP_USES_CAPABILITY);

    @Override
    public String operatorId() {
        return "capability.tool-discovery";
    }

    @Override
    public DimensionCategory category() {
        return DimensionCategory.CAPABILITY;
    }

    @Override
    public CognitionResult execute(CognitionQueryContext context) {
        String entityFqn = context.entityFqn();
        if (entityFqn == null || entityFqn.isBlank()) {
            return wrapFailure("缺少 entityFqn 参数");
        }

        Map<String, Object> config = context.operatorConfig();

        // 使用方（Agent/Task/Step）→ Capability：出边即得能力
        Object outResult = executeWithPort(() -> queryRelationsBySchema(
                entityFqn, RelationDirection.OUTBOUND, config, DEFAULT_CAPABILITY_RELATIONS, null));
        if (outResult instanceof CognitionResult cr) return cr;

        // 兼容：X 本身为 Capability 时，入边返回使用方（此处仅统计关系，不误报为能力）
        Object inResult = executeWithPort(() -> queryRelationsBySchema(
                entityFqn, RelationDirection.INBOUND, config, DEFAULT_CAPABILITY_RELATIONS, null));
        if (inResult instanceof CognitionResult cr) return cr;

        List<Map<String, Object>> capabilities = new ArrayList<>();
        collectCapabilities((List<RelationInstanceDto>) outResult, RelationDirection.OUTBOUND, capabilities);
        collectCapabilities((List<RelationInstanceDto>) inResult, RelationDirection.INBOUND, capabilities);

        Map<String, Object> resultData = new LinkedHashMap<>();
        resultData.put("capabilities", capabilities);
        resultData.put("count", capabilities.size());
        resultData.put("entityFqn", entityFqn);

        return CognitionResult.success(operatorId(), category(), resultData);
    }

    private void collectCapabilities(List<RelationInstanceDto> relations,
                                     RelationDirection direction,
                                     List<Map<String, Object>> capabilities) {
        if (relations == null || relations.isEmpty()) {
            return;
        }
        for (RelationInstanceDto rel : relations) {
            String peerFqn = resolvePeerFqn(rel, direction);
            if (peerFqn == null || peerFqn.isBlank()) {
                continue;
            }
            Map<String, Object> summary = buildSummary(peerFqn);
            if (summary != null) {
                capabilities.add(summary);
            }
        }
    }

    /**
     * 分层摘要：仅返回 fqn/name/description/protocolType，不含 interface_spec。
     */
    private Map<String, Object> buildSummary(String capabilityFqn) {
        Object portResult = executeWithPort(() -> metadataReadPort.getByFqn(capabilityFqn));
        if (portResult instanceof CognitionResult) {
            return null;
        }
        if (!(portResult instanceof MetadataEntityDto dto)) {
            return null;
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("fqn", dto.getFqn());
        summary.put("name", dto.getName());
        summary.put("description", dto.getDescription());
        Object callMethod = resolveContentValue(dto, "call_method");
        summary.put("protocolType", resolveProtocolType(callMethod));
        return summary;
    }

    private String resolveProtocolType(Object callMethod) {
        if (callMethod instanceof String s && !s.isBlank()) {
            String upper = s.trim().toUpperCase(Locale.ROOT);
            return switch (upper) {
                case "REST" -> "Http";
                case "MCP" -> "McpTool";
                case "CLI" -> "Cli";
                case "LOCAL", "INTERNAL" -> "LocalMethod";
                default -> s;
            };
        }
        return null;
    }
}
