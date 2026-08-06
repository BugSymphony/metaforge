package com.metaforge.agent.cognition.core.domain.model.aggregate;

import com.metaforge.agent.cognition.core.domain.model.entity.PerspectiveResult;
import com.metaforge.agent.cognition.core.domain.model.valueobject.ContextMeta;
import com.metaforge.agent.cognition.core.domain.model.valueobject.QueryParameters;
import com.metaforge.agent.cognition.core.domain.model.valueobject.TemplateId;

import java.util.ArrayList;
import java.util.List;

public class CognitionQuery {

    private TemplateId templateId;
    private QueryParameters queryParameters;
    private List<PerspectiveResult> perspectiveResults = new ArrayList<>();
    private ContextMeta contextMeta;

    public CognitionQuery(TemplateId templateId, QueryParameters queryParameters) {
        this.templateId = templateId;
        this.queryParameters = queryParameters;
    }

    public void execute() {}

    public void applyDepthTrim(int maxPerspectives) {}

    public void applyTokenBudget(int maxTokens) {}

    public TemplateId getTemplateId() { return templateId; }
    public void setTemplateId(TemplateId templateId) { this.templateId = templateId; }
    public QueryParameters getQueryParameters() { return queryParameters; }
    public void setQueryParameters(QueryParameters queryParameters) { this.queryParameters = queryParameters; }
    public List<PerspectiveResult> getPerspectiveResults() { return perspectiveResults; }
    public void setPerspectiveResults(List<PerspectiveResult> perspectiveResults) { this.perspectiveResults = perspectiveResults; }
    public void addPerspectiveResult(PerspectiveResult result) { this.perspectiveResults.add(result); }
    public ContextMeta getContextMeta() { return contextMeta; }
    public void setContextMeta(ContextMeta contextMeta) { this.contextMeta = contextMeta; }
}
