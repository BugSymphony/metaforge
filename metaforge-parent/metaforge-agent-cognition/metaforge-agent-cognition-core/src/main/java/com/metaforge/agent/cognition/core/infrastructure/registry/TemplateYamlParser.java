package com.metaforge.agent.cognition.core.infrastructure.registry;

import com.metaforge.agent.cognition.api.enums.AgentArchetype;
import com.metaforge.agent.cognition.core.domain.model.entity.OperatorDefinition;
import com.metaforge.agent.cognition.core.domain.model.entity.TemplateDefinition;
import com.metaforge.agent.cognition.core.domain.model.valueobject.InputSchema;
import com.metaforge.agent.cognition.core.domain.model.valueobject.OutputSchema;
import com.metaforge.agent.cognition.core.domain.model.valueobject.ScopeBehavior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class TemplateYamlParser {

    private static final Logger log = LoggerFactory.getLogger(TemplateYamlParser.class);
    private static final Set<String> ALL_ARCHETYPES = Arrays.stream(AgentArchetype.values())
            .map(Enum::name).collect(Collectors.toSet());

    public TemplateDefinition parse(String yamlContent) {
        try {
            Yaml yaml = new Yaml();
            Map<String, Object> raw = yaml.load(yamlContent);

            if (raw == null) {
                log.warn("YAML 内容为空");
                return null;
            }

            TemplateDefinition def = new TemplateDefinition();
            def.setTemplateId(getString(raw, "templateId"));
            def.setTemplateName(getString(raw, "templateName"));
            def.setDescription(getString(raw, "description"));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawOps = (List<Map<String, Object>>) raw.get("operators");
            if (rawOps != null) {
                List<OperatorDefinition> operators = new ArrayList<>();
                for (Map<String, Object> rawOp : rawOps) {
                    OperatorDefinition op = parseOperator(rawOp);
                    if (op != null) {
                        operators.add(op);
                    }
                }
                def.setOperators(operators);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> rawInput = (Map<String, Object>) raw.get("inputSchema");
            if (rawInput != null) {
                def.setInputSchema(parseInputSchema(rawInput));
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> rawScope = (Map<String, Object>) raw.get("scopeBehavior");
            if (rawScope != null) {
                def.setScopeBehavior(parseScopeBehavior(rawScope));
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> rawOutput = (Map<String, Object>) raw.get("outputSchema");
            if (rawOutput != null) {
                def.setOutputSchema(parseOutputSchema(rawOutput));
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> rawConfig = (Map<String, Object>) raw.get("config");
            if (rawConfig != null) {
                def.setConfig(rawConfig);
            }

            return def;
        } catch (Exception e) {
            log.warn("YAML 解析失败 (可能写入未完成): {}", e.getMessage());
            return null;
        }
    }

    private OperatorDefinition parseOperator(Map<String, Object> raw) {
        OperatorDefinition op = new OperatorDefinition();
        op.setOperatorId(getString(raw, "operatorId"));
        op.setName(getString(raw, "name"));
        op.setDescription(getString(raw, "description"));
        op.setPriority(getInt(raw, "priority", 0));
        op.setRequired(getBoolean(raw, "required", false));
        op.setTimeoutMs(getLong(raw, "timeoutMs", 10000));

        @SuppressWarnings("unchecked")
        List<String> archetypeList = (List<String>) raw.get("archetypes");
        if (archetypeList != null && !archetypeList.isEmpty()) {
            Set<AgentArchetype> archetypes = new HashSet<>();
            for (String a : archetypeList) {
                if (a != null && ALL_ARCHETYPES.contains(a.toUpperCase())) {
                    archetypes.add(AgentArchetype.valueOf(a.toUpperCase()));
                }
            }
            op.setArchetypes(archetypes);
        } else {
            op.setArchetypes(Set.of(AgentArchetype.values()));
        }

        return op;
    }

    private InputSchema parseInputSchema(Map<String, Object> raw) {
        InputSchema schema = new InputSchema();

        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) raw.get("properties");
        schema.setProperties(props != null ? props : new LinkedHashMap<>());

        @SuppressWarnings("unchecked")
        List<String> reqs = (List<String>) raw.get("required");
        schema.setRequired(reqs != null ? reqs : new ArrayList<>());

        return schema;
    }

    private ScopeBehavior parseScopeBehavior(Map<String, Object> raw) {
        ScopeBehavior sb = new ScopeBehavior();
        sb.setAcceptsScope(getBoolean(raw, "acceptsScope", false));
        sb.setScopeRequired(getBoolean(raw, "scopeRequired", false));
        sb.setProducesUpdatedScope(getBoolean(raw, "producesUpdatedScope", false));

        @SuppressWarnings("unchecked")
        List<String> fields = (List<String>) raw.get("scopeFields");
        sb.setScopeFields(fields != null ? fields : new ArrayList<>());

        sb.validate();
        return sb;
    }

    private OutputSchema parseOutputSchema(Map<String, Object> raw) {
        OutputSchema output = new OutputSchema();
        output.setType(getString(raw, "type"));

        @SuppressWarnings("unchecked")
        List<String> formats = (List<String>) raw.get("formats");
        output.setFormats(formats != null ? formats : new ArrayList<>());

        return output;
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object val = map.get(key);
        if (val instanceof Number n) {
            return n.intValue();
        }
        if (val instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private long getLong(Map<String, Object> map, String key, long defaultValue) {
        Object val = map.get(key);
        if (val instanceof Number n) {
            return n.longValue();
        }
        if (val instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object val = map.get(key);
        if (val instanceof Boolean b) {
            return b;
        }
        if (val instanceof String s) {
            return Boolean.parseBoolean(s);
        }
        return defaultValue;
    }
}
