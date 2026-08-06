package com.metaforge.agent.cognition.api.dto.response;

public class TaskBriefResponse {

    private ContextMeta contextMeta;
    private EntityProfile entityProfile;
    private DomainLocation domainLocation;
    private CompositionTree compositionTree;
    private RelationshipGraph relationshipGraph;
    private ConstraintSet constraintSet;
    private CapabilityCatalog capabilityCatalog;
    private FlowBlueprint flowBlueprint;
    private DecisionMatrix decisionMatrix;
    private ImpactTrace impactTrace;
    private PrerequisiteChain prerequisiteChain;

    public ContextMeta getContextMeta() { return contextMeta; }
    public void setContextMeta(ContextMeta contextMeta) { this.contextMeta = contextMeta; }
    public EntityProfile getEntityProfile() { return entityProfile; }
    public void setEntityProfile(EntityProfile entityProfile) { this.entityProfile = entityProfile; }
    public DomainLocation getDomainLocation() { return domainLocation; }
    public void setDomainLocation(DomainLocation domainLocation) { this.domainLocation = domainLocation; }
    public CompositionTree getCompositionTree() { return compositionTree; }
    public void setCompositionTree(CompositionTree compositionTree) { this.compositionTree = compositionTree; }
    public RelationshipGraph getRelationshipGraph() { return relationshipGraph; }
    public void setRelationshipGraph(RelationshipGraph relationshipGraph) { this.relationshipGraph = relationshipGraph; }
    public ConstraintSet getConstraintSet() { return constraintSet; }
    public void setConstraintSet(ConstraintSet constraintSet) { this.constraintSet = constraintSet; }
    public CapabilityCatalog getCapabilityCatalog() { return capabilityCatalog; }
    public void setCapabilityCatalog(CapabilityCatalog capabilityCatalog) { this.capabilityCatalog = capabilityCatalog; }
    public FlowBlueprint getFlowBlueprint() { return flowBlueprint; }
    public void setFlowBlueprint(FlowBlueprint flowBlueprint) { this.flowBlueprint = flowBlueprint; }
    public DecisionMatrix getDecisionMatrix() { return decisionMatrix; }
    public void setDecisionMatrix(DecisionMatrix decisionMatrix) { this.decisionMatrix = decisionMatrix; }
    public ImpactTrace getImpactTrace() { return impactTrace; }
    public void setImpactTrace(ImpactTrace impactTrace) { this.impactTrace = impactTrace; }
    public PrerequisiteChain getPrerequisiteChain() { return prerequisiteChain; }
    public void setPrerequisiteChain(PrerequisiteChain prerequisiteChain) { this.prerequisiteChain = prerequisiteChain; }
}
