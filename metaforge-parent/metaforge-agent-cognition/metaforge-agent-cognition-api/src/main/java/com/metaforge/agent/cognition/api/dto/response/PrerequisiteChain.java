package com.metaforge.agent.cognition.api.dto.response;

import java.util.List;

public class PrerequisiteChain {

    private String entityFqn;
    private List<PrerequisiteNode> dependencyTree;

    public String getEntityFqn() { return entityFqn; }
    public void setEntityFqn(String entityFqn) { this.entityFqn = entityFqn; }
    public List<PrerequisiteNode> getDependencyTree() { return dependencyTree; }
    public void setDependencyTree(List<PrerequisiteNode> dependencyTree) { this.dependencyTree = dependencyTree; }

    public static class PrerequisiteNode {
        private String entityFqn;
        private String entityName;
        private String dependencyType;
        private boolean blocking;
        private String entityStatus;
        private int level;
        private List<PrerequisiteNode> children;

        public String getEntityFqn() { return entityFqn; }
        public void setEntityFqn(String entityFqn) { this.entityFqn = entityFqn; }
        public String getEntityName() { return entityName; }
        public void setEntityName(String entityName) { this.entityName = entityName; }
        public String getDependencyType() { return dependencyType; }
        public void setDependencyType(String dependencyType) { this.dependencyType = dependencyType; }
        public boolean isBlocking() { return blocking; }
        public void setBlocking(boolean blocking) { this.blocking = blocking; }
        public String getEntityStatus() { return entityStatus; }
        public void setEntityStatus(String entityStatus) { this.entityStatus = entityStatus; }
        public int getLevel() { return level; }
        public void setLevel(int level) { this.level = level; }
        public List<PrerequisiteNode> getChildren() { return children; }
        public void setChildren(List<PrerequisiteNode> children) { this.children = children; }
    }
}
