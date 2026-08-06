package com.metaforge.agent.cognition.api.dto.response;

import java.util.Map;

public class GuidanceResult {

    private String templateId;
    private ContextMeta contextMeta;
    private Map<String, Object> perspectives;

    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public ContextMeta getContextMeta() { return contextMeta; }
    public void setContextMeta(ContextMeta contextMeta) { this.contextMeta = contextMeta; }
    public Map<String, Object> getPerspectives() { return perspectives; }
    public void setPerspectives(Map<String, Object> perspectives) { this.perspectives = perspectives; }
}
