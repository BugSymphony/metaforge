package com.metaforge.agent.cognition.api.dto.response;

public class StepGuideResponse {

    private ContextMeta contextMeta;
    private EntityProfile entityProfile;
    private ConstraintSet constraintSet;
    private CapabilityCatalog capabilityCatalog;
    private DecisionMatrix decisionMatrix;
    private ImpactTrace impactTrace;
    private RelationshipGraph relationshipGraph;
    private AdjacentContext adjacentContext;

    public ContextMeta getContextMeta() { return contextMeta; }
    public void setContextMeta(ContextMeta contextMeta) { this.contextMeta = contextMeta; }
    public EntityProfile getEntityProfile() { return entityProfile; }
    public void setEntityProfile(EntityProfile entityProfile) { this.entityProfile = entityProfile; }
    public ConstraintSet getConstraintSet() { return constraintSet; }
    public void setConstraintSet(ConstraintSet constraintSet) { this.constraintSet = constraintSet; }
    public CapabilityCatalog getCapabilityCatalog() { return capabilityCatalog; }
    public void setCapabilityCatalog(CapabilityCatalog capabilityCatalog) { this.capabilityCatalog = capabilityCatalog; }
    public DecisionMatrix getDecisionMatrix() { return decisionMatrix; }
    public void setDecisionMatrix(DecisionMatrix decisionMatrix) { this.decisionMatrix = decisionMatrix; }
    public ImpactTrace getImpactTrace() { return impactTrace; }
    public void setImpactTrace(ImpactTrace impactTrace) { this.impactTrace = impactTrace; }
    public RelationshipGraph getRelationshipGraph() { return relationshipGraph; }
    public void setRelationshipGraph(RelationshipGraph relationshipGraph) { this.relationshipGraph = relationshipGraph; }
    public AdjacentContext getAdjacentContext() { return adjacentContext; }
    public void setAdjacentContext(AdjacentContext adjacentContext) { this.adjacentContext = adjacentContext; }
}
