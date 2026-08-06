package com.metaforge.agent.cognition.core.domain.model.valueobject;

import com.metaforge.agent.cognition.api.enums.*;

import java.util.List;
import java.util.Map;

public class QueryParameters {

    private List<String> bundleFqns;
    private String entityFqn;
    private List<String> entityTypes;
    private String subjectDomainFqn;
    private ScopeMode scopeMode;
    private CognitionDepth cognitionDepth;
    private AgentArchetype agentArchetype;
    private int maxTokens;
    private String expand;
    private OutputFormat format;
    private String cursor;
    private Integer pageSize;
    private Map<String, String> contextParameters;

    public List<String> getBundleFqns() { return bundleFqns; }
    public void setBundleFqns(List<String> bundleFqns) { this.bundleFqns = bundleFqns; }
    public String getEntityFqn() { return entityFqn; }
    public void setEntityFqn(String entityFqn) { this.entityFqn = entityFqn; }
    public List<String> getEntityTypes() { return entityTypes; }
    public void setEntityTypes(List<String> entityTypes) { this.entityTypes = entityTypes; }
    public String getSubjectDomainFqn() { return subjectDomainFqn; }
    public void setSubjectDomainFqn(String subjectDomainFqn) { this.subjectDomainFqn = subjectDomainFqn; }
    public ScopeMode getScopeMode() { return scopeMode; }
    public void setScopeMode(ScopeMode scopeMode) { this.scopeMode = scopeMode; }
    public CognitionDepth getCognitionDepth() { return cognitionDepth; }
    public void setCognitionDepth(CognitionDepth cognitionDepth) { this.cognitionDepth = cognitionDepth; }
    public AgentArchetype getAgentArchetype() { return agentArchetype; }
    public void setAgentArchetype(AgentArchetype agentArchetype) { this.agentArchetype = agentArchetype; }
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    public String getExpand() { return expand; }
    public void setExpand(String expand) { this.expand = expand; }
    public OutputFormat getFormat() { return format; }
    public void setFormat(OutputFormat format) { this.format = format; }
    public String getCursor() { return cursor; }
    public void setCursor(String cursor) { this.cursor = cursor; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    public Map<String, String> getContextParameters() { return contextParameters; }
    public void setContextParameters(Map<String, String> contextParameters) { this.contextParameters = contextParameters; }
}
