package com.metaforge.agent.cognition.core.domain.model.entity;

import com.metaforge.agent.cognition.core.domain.model.valueobject.InputSchema;
import com.metaforge.agent.cognition.core.domain.model.valueobject.OutputSchema;
import com.metaforge.agent.cognition.core.domain.model.valueobject.ScopeBehavior;

import java.util.*;
import java.util.regex.Pattern;

public class TemplateDefinition {

    private static final Pattern TEMPLATE_ID_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]+");

    private String templateId;
    private String templateName;
    private String description;
    private List<OperatorDefinition> operators = new ArrayList<>();
    private InputSchema inputSchema = new InputSchema();
    private ScopeBehavior scopeBehavior = new ScopeBehavior();
    private OutputSchema outputSchema = new OutputSchema();
    private Map<String, Object> config = new LinkedHashMap<>();

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<OperatorDefinition> getOperators() {
        return operators;
    }

    public void setOperators(List<OperatorDefinition> operators) {
        this.operators = operators != null ? operators : new ArrayList<>();
    }

    public InputSchema getInputSchema() {
        return inputSchema;
    }

    public void setInputSchema(InputSchema inputSchema) {
        this.inputSchema = inputSchema != null ? inputSchema : new InputSchema();
    }

    public ScopeBehavior getScopeBehavior() {
        return scopeBehavior;
    }

    public void setScopeBehavior(ScopeBehavior scopeBehavior) {
        this.scopeBehavior = scopeBehavior != null ? scopeBehavior : new ScopeBehavior();
    }

    public OutputSchema getOutputSchema() {
        return outputSchema;
    }

    public void setOutputSchema(OutputSchema outputSchema) {
        this.outputSchema = outputSchema != null ? outputSchema : new OutputSchema();
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config != null ? config : new LinkedHashMap<>();
    }

    /**
     * 模板级全局配置——config.global 或 config 顶层数据（向后兼容单层结构，如 ORIENT levelAliases）。
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getGlobalConfig() {
        if (config == null) {
            return new LinkedHashMap<>();
        }
        Object global = config.get("global");
        if (global instanceof Map<?, ?> gm) {
            return (Map<String, Object>) gm;
        }
        return config;
    }

    /**
     * 算子级配置——config.operators.get(operatorId)；无则返回空 Map。
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getOperatorConfig(String operatorId) {
        if (config == null) {
            return new LinkedHashMap<>();
        }
        Object operatorsCfg = config.get("operators");
        if (operatorsCfg instanceof Map<?, ?> opsMap) {
            Object opCfg = opsMap.get(operatorId);
            if (opCfg instanceof Map<?, ?> m) {
                return (Map<String, Object>) m;
            }
        }
        return new LinkedHashMap<>();
    }

    public void validate() {
        if (templateId == null || templateId.isBlank()) {
            throw new IllegalArgumentException("templateId 不能为空");
        }
        if (!TEMPLATE_ID_PATTERN.matcher(templateId).matches()) {
            throw new IllegalArgumentException("templateId 格式无效: '" + templateId
                    + "'，须满足 [A-Z][A-Z0-9_]+");
        }
        if (operators.isEmpty()) {
            throw new IllegalArgumentException("模板 " + templateId + " 未声明任何算子");
        }
        for (OperatorDefinition op : operators) {
            op.validate();
        }
        if (scopeBehavior != null) {
            scopeBehavior.validate();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TemplateDefinition that)) return false;
        return Objects.equals(templateId, that.templateId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(templateId);
    }

    @Override
    public String toString() {
        return "TemplateDefinition{id='" + templateId + "', name='" + templateName
                + "', operators=" + operators.size() + '}';
    }
}
