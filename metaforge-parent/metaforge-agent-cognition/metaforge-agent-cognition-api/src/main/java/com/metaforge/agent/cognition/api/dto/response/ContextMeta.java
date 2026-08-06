package com.metaforge.agent.cognition.api.dto.response;

import com.metaforge.agent.cognition.api.dto.DataVersionAnchor;
import com.metaforge.agent.cognition.api.enums.AgentArchetype;
import com.metaforge.agent.cognition.api.enums.CognitionDepth;
import com.metaforge.agent.cognition.api.enums.ContextMode;
import com.metaforge.agent.cognition.api.enums.PerspectiveCode;
import com.metaforge.agent.cognition.api.enums.ScopeMode;

import java.time.Instant;
import java.util.List;

public class ContextMeta {

    private List<String> bundleFqns;
    private String entityFqn;
    private ContextMode contextMode;
    private ScopeMode scopeMode;
    private CognitionDepth cognitionDepth;
    private AgentArchetype agentArchetype;
    private List<String> appliedPerspectives;
    private List<String> skippedPerspectives;
    private List<String> skipReasons;
    private List<DataVersionAnchor> dataVersionAnchors;
    private AdjacentContext adjacentContext;
    private Long totalTokenCount;
    private Boolean tokenTrimmed;
    private Boolean truncated;
    private List<TruncationNote> truncations;
    private Instant queriedAt;

    public List<String> getBundleFqns() { return bundleFqns; }
    public void setBundleFqns(List<String> bundleFqns) { this.bundleFqns = bundleFqns; }
    public String getEntityFqn() { return entityFqn; }
    public void setEntityFqn(String entityFqn) { this.entityFqn = entityFqn; }
    public ContextMode getContextMode() { return contextMode; }
    public void setContextMode(ContextMode contextMode) { this.contextMode = contextMode; }
    public ScopeMode getScopeMode() { return scopeMode; }
    public void setScopeMode(ScopeMode scopeMode) { this.scopeMode = scopeMode; }
    public CognitionDepth getCognitionDepth() { return cognitionDepth; }
    public void setCognitionDepth(CognitionDepth cognitionDepth) { this.cognitionDepth = cognitionDepth; }
    public AgentArchetype getAgentArchetype() { return agentArchetype; }
    public void setAgentArchetype(AgentArchetype agentArchetype) { this.agentArchetype = agentArchetype; }
    public List<String> getAppliedPerspectives() { return appliedPerspectives; }
    public void setAppliedPerspectives(List<String> appliedPerspectives) { this.appliedPerspectives = appliedPerspectives; }
    public List<String> getSkippedPerspectives() { return skippedPerspectives; }
    public void setSkippedPerspectives(List<String> skippedPerspectives) { this.skippedPerspectives = skippedPerspectives; }
    public List<String> getSkipReasons() { return skipReasons; }
    public void setSkipReasons(List<String> skipReasons) { this.skipReasons = skipReasons; }
    public List<DataVersionAnchor> getDataVersionAnchors() { return dataVersionAnchors; }
    public void setDataVersionAnchors(List<DataVersionAnchor> dataVersionAnchors) { this.dataVersionAnchors = dataVersionAnchors; }
    public AdjacentContext getAdjacentContext() { return adjacentContext; }
    public void setAdjacentContext(AdjacentContext adjacentContext) { this.adjacentContext = adjacentContext; }
    public Long getTotalTokenCount() { return totalTokenCount; }
    public void setTotalTokenCount(Long totalTokenCount) { this.totalTokenCount = totalTokenCount; }
    public Boolean getTokenTrimmed() { return tokenTrimmed; }
    public void setTokenTrimmed(Boolean tokenTrimmed) { this.tokenTrimmed = tokenTrimmed; }
    public Boolean getTruncated() { return truncated; }
    public void setTruncated(Boolean truncated) { this.truncated = truncated; }
    public List<TruncationNote> getTruncations() { return truncations; }
    public void setTruncations(List<TruncationNote> truncations) { this.truncations = truncations; }
    public Instant getQueriedAt() { return queriedAt; }
    public void setQueriedAt(Instant queriedAt) { this.queriedAt = queriedAt; }

    public record TruncationNote(
            PerspectiveCode perspective,
            String reason) {
    }
}
