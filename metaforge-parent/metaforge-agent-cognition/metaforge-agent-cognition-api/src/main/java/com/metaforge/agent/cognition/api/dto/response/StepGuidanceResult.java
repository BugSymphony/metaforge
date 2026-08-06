package com.metaforge.agent.cognition.api.dto.response;

public record StepGuidanceResult(
        EntityProfile entityProfile,
        ConstraintSet constraintSet,
        CapabilityCatalog capabilityCatalog,
        DecisionMatrix decisionMatrix,
        ImpactTrace impactTrace,
        RelationshipGraph relationshipGraph,
        AdjacentContext adjacentContext) {
}
