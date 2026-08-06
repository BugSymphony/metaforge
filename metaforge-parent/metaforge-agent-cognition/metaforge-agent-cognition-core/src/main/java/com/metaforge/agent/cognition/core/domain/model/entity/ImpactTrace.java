package com.metaforge.agent.cognition.core.domain.model.entity;

import java.util.List;
import java.util.Map;

public class ImpactTrace {
    private String sourceFqn; private Map<Integer, List<ImpactEntity>> forwardImpact;
    private Map<Integer, List<ImpactEntity>> backwardDependency; private List<ImpactPath> impactPaths;
    public static class ImpactEntity {
        private String fqn; private String name; private String entitySchemaFqn; private int depth;
        public String getFqn() { return fqn; } public void setFqn(String f) { this.fqn = f; }
        public String getName() { return name; } public void setName(String n) { this.name = n; }
        public String getEntitySchemaFqn() { return entitySchemaFqn; } public void setEntitySchemaFqn(String e) { this.entitySchemaFqn = e; }
        public int getDepth() { return depth; } public void setDepth(int d) { this.depth = d; }
    }
    public static class ImpactPath {
        private String targetFqn; private int pathLength; private List<String> hopEntities;
        private List<String> hopRelations; private String semanticDescription;
        public String getTargetFqn() { return targetFqn; } public void setTargetFqn(String t) { this.targetFqn = t; }
        public int getPathLength() { return pathLength; } public void setPathLength(int p) { this.pathLength = p; }
        public List<String> getHopEntities() { return hopEntities; } public void setHopEntities(List<String> h) { this.hopEntities = h; }
        public List<String> getHopRelations() { return hopRelations; } public void setHopRelations(List<String> h) { this.hopRelations = h; }
        public String getSemanticDescription() { return semanticDescription; } public void setSemanticDescription(String s) { this.semanticDescription = s; }
    }
    public String getSourceFqn() { return sourceFqn; } public void setSourceFqn(String s) { this.sourceFqn = s; }
    public Map<Integer, List<ImpactEntity>> getForwardImpact() { return forwardImpact; } public void setForwardImpact(Map<Integer, List<ImpactEntity>> f) { this.forwardImpact = f; }
    public Map<Integer, List<ImpactEntity>> getBackwardDependency() { return backwardDependency; } public void setBackwardDependency(Map<Integer, List<ImpactEntity>> b) { this.backwardDependency = b; }
    public List<ImpactPath> getImpactPaths() { return impactPaths; } public void setImpactPaths(List<ImpactPath> i) { this.impactPaths = i; }
}
