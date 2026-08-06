package com.metaforge.agent.cognition.api.dto.response;

import java.util.List;

public class CompositionTree {

    private String rootFqn;
    private String direction;
    private TreeNode root;
    private boolean truncated;
    private String truncatedReason;

    public String getRootFqn() { return rootFqn; }
    public void setRootFqn(String rootFqn) { this.rootFqn = rootFqn; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public TreeNode getRoot() { return root; }
    public void setRoot(TreeNode root) { this.root = root; }
    public boolean isTruncated() { return truncated; }
    public void setTruncated(boolean truncated) { this.truncated = truncated; }
    public String getTruncatedReason() { return truncatedReason; }
    public void setTruncatedReason(String truncatedReason) { this.truncatedReason = truncatedReason; }

    public static class TreeNode {
        private String fqn;
        private String name;
        private String entitySchemaFqn;
        private int depth;
        private List<TreeNode> children;
        private List<TreeNode> parentChain;

        public String getFqn() { return fqn; }
        public void setFqn(String fqn) { this.fqn = fqn; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEntitySchemaFqn() { return entitySchemaFqn; }
        public void setEntitySchemaFqn(String entitySchemaFqn) { this.entitySchemaFqn = entitySchemaFqn; }
        public int getDepth() { return depth; }
        public void setDepth(int depth) { this.depth = depth; }
        public List<TreeNode> getChildren() { return children; }
        public void setChildren(List<TreeNode> children) { this.children = children; }
        public List<TreeNode> getParentChain() { return parentChain; }
        public void setParentChain(List<TreeNode> parentChain) { this.parentChain = parentChain; }
    }
}
