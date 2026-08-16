package com.metaforge.agent.cognition.core.domain.model.aggregate;

import com.metaforge.agent.cognition.api.dto.request.CognitionRequest;
import com.metaforge.agent.cognition.api.dto.request.Scope;
import com.metaforge.agent.cognition.api.enums.AgentArchetype;
import com.metaforge.agent.cognition.api.enums.CognitionDepth;
import com.metaforge.agent.cognition.api.enums.OutputFormat;
import com.metaforge.agent.cognition.api.spi.CognitionResult;
import com.metaforge.agent.cognition.core.domain.model.entity.OperatorDefinition;
import com.metaforge.agent.cognition.core.domain.model.entity.TemplateDefinition;
import com.metaforge.agent.cognition.core.domain.model.valueobject.OperatorId;
import com.metaforge.agent.cognition.core.domain.model.valueobject.ScopeBehavior;
import com.metaforge.agent.cognition.core.domain.model.valueobject.TemplateId;
import com.metaforge.agent.cognition.core.domain.model.valueobject.TokenBudget;

import java.util.*;
import java.util.stream.Collectors;

public class CognitionQuery {

    private TemplateId templateId;
    private CognitionRequest request;
    private TemplateDefinition templateDefinition;
    private List<OperatorDefinition> operators;
    private Scope scope;
    private OutputFormat outputFormat;
    private AgentArchetype agentArchetype;
    private CognitionDepth cognitionDepth;
    private TokenBudget tokenBudget;
    private Map<OperatorId, CognitionResult> executionResults;

    private final List<String> skippedEntities = new ArrayList<>();
    private final List<String> truncatedPerspectives = new ArrayList<>();

    public CognitionQuery(TemplateId templateId, CognitionRequest request) {
        this.templateId = templateId;
        this.request = request;
        this.executionResults = new LinkedHashMap<>();
    }

    public void loadTemplate(TemplateDefinition definition) {
        this.templateDefinition = definition;
        this.operators = new ArrayList<>(definition.getOperators());
        ScopeBehavior sb = definition.getScopeBehavior();
        this.outputFormat = request.resolvedFormat();
        if (this.outputFormat == null) {
            throw new com.metaforge.agent.cognition.core.domain.exception.InvalidFormatException(request.format());
        }
        this.agentArchetype = request.agentArchetype() != null ? request.agentArchetype() : AgentArchetype.EXECUTION;
        this.cognitionDepth = request.cognitionDepth() != null ? request.cognitionDepth() : CognitionDepth.L2;
        this.tokenBudget = request.maxTokens() != null ? new TokenBudget(request.maxTokens()) : new TokenBudget(8000);
        this.scope = request.scope() != null ? request.scope() : Scope.EMPTY;

        if (tokenBudget.autoDowngrade() != null) {
            this.cognitionDepth = CognitionDepth.L1;
        }
    }

    public List<OperatorDefinition> filterByOperators() {
        Object requested = request.params().get("selectOperators");
        List<String> selection = null;
        if (requested instanceof List<?> list && !list.isEmpty()) {
            selection = list.stream().map(String::valueOf).collect(Collectors.toList());
        } else {
            // 请求未指定 selectOperators 时，使用模板 inputSchema.selectOperators.default（若有）
            selection = resolveDefaultSelectOperators();
        }
        if (selection == null || selection.isEmpty()) {
            return operators;
        }
        Set<String> ids = new HashSet<>(selection);
        List<OperatorDefinition> filtered = operators.stream()
                .filter(op -> ids.contains(op.getOperatorId()))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            throw new com.metaforge.agent.cognition.api.exception.InvalidOperatorSelectionException(
                    ids, templateId.value());
        }

        this.operators = filtered;
        return filtered;
    }

    /**
     * 解析模板 inputSchema.selectOperators 声明的默认选择；未声明/为空时返回 null（执行全部算子）。
     */
    @SuppressWarnings("unchecked")
    private List<String> resolveDefaultSelectOperators() {
        if (templateDefinition == null || templateDefinition.getInputSchema() == null
                || templateDefinition.getInputSchema().getProperties() == null) {
            return null;
        }
        Object sel = templateDefinition.getInputSchema().getProperties().get("selectOperators");
        if (!(sel instanceof Map<?, ?> selMap)) {
            return null;
        }
        Object def = selMap.get("default");
        if (!(def instanceof List<?> list) || list.isEmpty()) {
            return null;
        }
        return list.stream().map(String::valueOf).filter(s -> !s.isBlank()).toList();
    }

    public List<OperatorDefinition> filterByArchetype() {
        if (agentArchetype == null) {
            return operators;
        }
        List<OperatorDefinition> filtered = operators.stream()
                .filter(op -> op.supportsArchetype(agentArchetype))
                .collect(Collectors.toList());

        if (operators.isEmpty()) {
            this.operators = filtered;
            return filtered;
        }

        if (filtered.isEmpty()) {
            throw new com.metaforge.agent.cognition.core.domain.exception.ArchetypeNotSupportedException(
                    agentArchetype.name(), templateId.value());
        }

        this.operators = filtered;
        return filtered;
    }

    public Map<OperatorId, CognitionResult> getExecutionResults() {
        return executionResults;
    }

    public void addExecutionResult(OperatorId operatorId, CognitionResult result) {
        this.executionResults.put(operatorId, result);
    }

    public TemplateId getTemplateId() { return templateId; }
    public CognitionRequest getRequest() { return request; }
    public TemplateDefinition getTemplateDefinition() { return templateDefinition; }
    public List<OperatorDefinition> getOperators() { return operators; }
    public Scope getScope() { return scope; }
    public OutputFormat getOutputFormat() { return outputFormat; }
    public AgentArchetype getAgentArchetype() { return agentArchetype; }
    public CognitionDepth getCognitionDepth() { return cognitionDepth; }
    public TokenBudget getTokenBudget() { return tokenBudget; }

    public List<String> getSkippedEntities() {
        return Collections.unmodifiableList(skippedEntities);
    }

    public List<String> getTruncatedPerspectives() {
        return Collections.unmodifiableList(truncatedPerspectives);
    }

    public void addSkippedEntity(String entityFqn) {
        this.skippedEntities.add(entityFqn);
    }

    public void setOperators(List<OperatorDefinition> operators) {
        this.operators = operators != null ? operators : new ArrayList<>();
    }

    public void addTruncatedPerspective(String perspective) {
        this.truncatedPerspectives.add(perspective);
    }
}
