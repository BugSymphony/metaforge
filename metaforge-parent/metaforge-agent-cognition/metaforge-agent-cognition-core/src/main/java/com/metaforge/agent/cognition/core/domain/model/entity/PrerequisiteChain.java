package com.metaforge.agent.cognition.core.domain.model.entity;

import java.util.List;

public class PrerequisiteChain {
    private String entityFqn; private List<PrerequisiteNode> dependencyTree;
    public static class PrerequisiteNode {
        private String entityFqn; private String entityName; private String dependencyType;
        private boolean blocking; private String entityStatus; private int level; private List<PrerequisiteNode> children;
        public String getEntityFqn() { return entityFqn; } public void setEntityFqn(String e) { this.entityFqn = e; }
        public String getEntityName() { return entityName; } public void setEntityName(String e) { this.entityName = e; }
        public String getDependencyType() { return dependencyType; } public void setDependencyType(String d) { this.dependencyType = d; }
        public boolean isBlocking() { return blocking; } public void setBlocking(boolean b) { this.blocking = b; }
        public String getEntityStatus() { return entityStatus; } public void setEntityStatus(String e) { this.entityStatus = e; }
        public int getLevel() { return level; } public void setLevel(int l) { this.level = l; }
        public List<PrerequisiteNode> getChildren() { return children; } public void setChildren(List<PrerequisiteNode> c) { this.children = c; }
    }
    public String getEntityFqn() { return entityFqn; } public void setEntityFqn(String e) { this.entityFqn = e; }
    public List<PrerequisiteNode> getDependencyTree() { return dependencyTree; } public void setDependencyTree(List<PrerequisiteNode> d) { this.dependencyTree = d; }
}
