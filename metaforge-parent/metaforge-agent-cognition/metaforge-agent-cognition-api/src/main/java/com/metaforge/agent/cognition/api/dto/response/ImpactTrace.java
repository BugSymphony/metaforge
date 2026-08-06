package com.metaforge.agent.cognition.api.dto.response;

import java.util.List;
import java.util.Map;

public class ImpactTrace {

    private String sourceFqn;
    private Map<Integer, List<ImpactEntity>> forwardImpact;
    private Map<Integer, List<ImpactEntity>> backwardDependency;
    private List<ImpactPath> impactPaths;

    public String getSourceFqn() { return sourceFqn; }
    public void setSourceFqn(String sourceFqn) { this.sourceFqn = sourceFqn; }
    public Map<Integer, List<ImpactEntity>> getForwardImpact() { return forwardImpact; }
    public void setForwardImpact(Map<Integer, List<ImpactEntity>> forwardImpact) { this.forwardImpact = forwardImpact; }
    public Map<Integer, List<ImpactEntity>> getBackwardDependency() { return backwardDependency; }
    public void setBackwardDependency(Map<Integer, List<ImpactEntity>> backwardDependency) { this.backwardDependency = backwardDependency; }
    public List<ImpactPath> getImpactPaths() { return impactPaths; }
    public void setImpactPaths(List<ImpactPath> impactPaths) { this.impactPaths = impactPaths; }

    public static class ImpactEntity {
        private String fqn;
        private String name;
        private String entitySchemaFqn;
        private int depth;

        public String getFqn() { return fqn; }
        public void setFqn(String fqn) { this.fqn = fqn; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEntitySchemaFqn() { return entitySchemaFqn; }
        public void setEntitySchemaFqn(String entitySchemaFqn) { this.entitySchemaFqn = entitySchemaFqn; }
        public int getDepth() { return depth; }
        public void setDepth(int depth) { this.depth = depth; }
    }

    public static class ImpactPath {
        private String targetFqn;
        private int pathLength;
        private List<String> hopEntities;
        private List<String> hopRelations;
        private String semanticDescription;

        public String getTargetFqn() { return targetFqn; }
        public void setTargetFqn(String targetFqn) { this.targetFqn = targetFqn; }
        public int getPathLength() { return pathLength; }
        public void setPathLength(int pathLength) { this.pathLength = pathLength; }
        public List<String> getHopEntities() { return hopEntities; }
        public void setHopEntities(List<String> hopEntities) { this.hopEntities = hopEntities; }
        public List<String> getHopRelations() { return hopRelations; }
        public void setHopRelations(List<String> hopRelations) { this.hopRelations = hopRelations; }
        public String getSemanticDescription() { return semanticDescription; }
        public void setSemanticDescription(String semanticDescription) { this.semanticDescription = semanticDescription; }
    }
}
