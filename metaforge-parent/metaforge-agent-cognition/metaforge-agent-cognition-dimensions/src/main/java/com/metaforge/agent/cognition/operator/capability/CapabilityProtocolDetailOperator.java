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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 协议细节——读取能力 content.interface_spec 结构化展开，同时按关系 schema 前缀
 * （默认 {@code metaforge:1.0.0.protocol.}）查询能力引用的协议实例（Capability → protocol.X）。
 */
@Component
public class CapabilityProtocolDetailOperator extends AbstractCognitionOperator {

    private static final String TYPE_HTTP = "Http";
    private static final String TYPE_MCP = "McpTool";
    private static final String TYPE_CLI = "Cli";
    private static final String TYPE_LOCAL = "LocalMethod";

    @Override
    public String operatorId() {
        return "capability.protocol-detail";
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

        final String fqn = entityFqn;
        Object entityResult = executeWithPort(() -> metadataReadPort.getByFqn(fqn));
        if (entityResult instanceof CognitionResult cr) return cr;

        Map<String, Object> config = context.operatorConfig();
        Object relResult = executeWithPort(() -> queryRelationsBySchema(
                fqn, RelationDirection.OUTBOUND, config,
                Collections.emptyList(), MetaforgeLibraryFqns.PROTOCOL_RELATION_PREFIX));
        if (relResult instanceof CognitionResult cr) return cr;

        Object interfaceSpec = null;
        if (entityResult instanceof MetadataEntityDto dto) {
            interfaceSpec = resolveContentValue(dto, "interface_spec");
        }

        List<Map<String, Object>> protocolInstances = new ArrayList<>();
        if (relResult instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof RelationInstanceDto rel) {
                    Map<String, Object> instance = buildProtocolInstance(rel.getTargetEntityFqn());
                    if (instance != null) {
                        protocolInstances.add(instance);
                    }
                }
            }
        }

        Map<String, Object> protocol = resolveProtocol(interfaceSpec);

        Map<String, Object> resultData = new LinkedHashMap<>();
        resultData.put("protocol", protocol);
        resultData.put("interface_spec", interfaceSpec);
        resultData.put("protocol_subtypes", protocolInstances);
        resultData.put("entityFqn", entityFqn);

        return CognitionResult.success(operatorId(), category(), resultData);
    }

    /**
     * 解析协议实例实体为结构化摘要（fqn + entitySchemaFqn + content）。
     */
    private Map<String, Object> buildProtocolInstance(String protocolFqn) {
        if (protocolFqn == null || protocolFqn.isBlank()) {
            return null;
        }
        Object portResult = executeWithPort(() -> metadataReadPort.getByFqn(protocolFqn));
        if (portResult instanceof CognitionResult) {
            return null;
        }
        if (!(portResult instanceof MetadataEntityDto dto)) {
            return null;
        }
        Map<String, Object> instance = new LinkedHashMap<>();
        instance.put("fqn", dto.getFqn());
        instance.put("name", dto.getName());
        instance.put("description", dto.getDescription());
        instance.put("entitySchemaFqn", dto.getEntitySchemaFqn());
        if (dto.getContent() != null) {
            instance.putAll(dto.getContent());
        }
        return instance;
    }

    private Map<String, Object> resolveProtocol(Object interfaceSpec) {
        if (!(interfaceSpec instanceof Map<?, ?> rawMap)) {
            return null;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> spec = (Map<String, Object>) rawMap;
        String type = resolveType(spec);

        Map<String, Object> protocol = new LinkedHashMap<>();
        protocol.put("type", type);
        protocol.putAll(type == TYPE_HTTP ? httpFields(spec)
                : type == TYPE_MCP ? mcpFields(spec)
                : type == TYPE_CLI ? cliFields(spec)
                : localFields(spec));
        return protocol;
    }

    private String resolveType(Map<String, Object> spec) {
        for (String key : new String[]{"type", "interfaceType", "protocolType"}) {
            if (spec.get(key) instanceof String str && !str.isBlank()) {
                String normalized = normalizeType(str);
                if (normalized != null) {
                    return normalized;
                }
            }
        }

        if (spec.containsKey("endpoint") || spec.containsKey("url")) return TYPE_HTTP;
        if (spec.containsKey("server_name") || spec.containsKey("tool_name")) return TYPE_MCP;
        if (spec.containsKey("command") || spec.containsKey("cli")) return TYPE_CLI;
        if (spec.containsKey("method_ref") || spec.containsKey("className")) return TYPE_LOCAL;
        return TYPE_HTTP;
    }

    private String normalizeType(String raw) {
        String upper = raw.trim().toUpperCase(Locale.ROOT);
        if (upper.equals("HTTP") || upper.equals("REST") || upper.contains("HTTP")) return TYPE_HTTP;
        if (upper.equals("MCP") || upper.equals("MCPTOOL") || upper.contains("MCP")) return TYPE_MCP;
        if (upper.equals("CLI") || upper.contains("CLI")) return TYPE_CLI;
        if (upper.equals("LOCALMETHOD") || upper.equals("LOCAL") || upper.contains("LOCAL")) return TYPE_LOCAL;
        return null;
    }

    private Map<String, Object> httpFields(Map<String, Object> spec) {
        Map<String, Object> fields = new LinkedHashMap<>();
        putIfPresent(fields, "endpoint", spec, "endpoint", "url");
        putIfPresent(fields, "method", spec, "method", "httpMethod");
        putIfPresent(fields, "headers", spec, "headers");
        putIfPresent(fields, "input_schema", spec, "input_schema", "requestSchema");
        putIfPresent(fields, "output_schema", spec, "output_schema", "responseSchema");
        return fields;
    }

    private Map<String, Object> mcpFields(Map<String, Object> spec) {
        Map<String, Object> fields = new LinkedHashMap<>();
        putIfPresent(fields, "server_name", spec, "server_name", "server");
        putIfPresent(fields, "tool_name", spec, "tool_name", "name");
        putIfPresent(fields, "arguments_schema", spec, "arguments_schema", "input_schema");
        putIfPresent(fields, "output_schema", spec, "output_schema");
        return fields;
    }

    private Map<String, Object> cliFields(Map<String, Object> spec) {
        Map<String, Object> fields = new LinkedHashMap<>();
        putIfPresent(fields, "command", spec, "command");
        putIfPresent(fields, "args_schema", spec, "args_schema", "arguments_schema");
        putIfPresent(fields, "working_dir", spec, "working_dir", "workingDirectory");
        return fields;
    }

    private Map<String, Object> localFields(Map<String, Object> spec) {
        Map<String, Object> fields = new LinkedHashMap<>();
        putIfPresent(fields, "method_ref", spec, "method_ref", "method");
        putIfPresent(fields, "className", spec, "className", "class");
        putIfPresent(fields, "input_schema", spec, "input_schema");
        putIfPresent(fields, "output_schema", spec, "output_schema");
        return fields;
    }

    private void putIfPresent(Map<String, Object> target, String targetKey,
                              Map<String, Object> source, String... sourceKeys) {
        for (String key : sourceKeys) {
            if (source.containsKey(key)) {
                target.put(targetKey, source.get(key));
                return;
            }
        }
    }
}
