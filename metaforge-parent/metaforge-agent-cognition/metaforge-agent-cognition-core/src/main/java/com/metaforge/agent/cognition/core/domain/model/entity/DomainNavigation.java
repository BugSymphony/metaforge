package com.metaforge.agent.cognition.core.domain.model.entity;

import java.util.List;

public class DomainNavigation {
    private String anchorFqn; private String currentLevel; private List<NavNode> children;
    private boolean hasMore; private String nextCursor;
    public static class NavNode {
        private String fqn; private String name; private String description; private int childCount; private boolean hasMoreChildren;
        public String getFqn() { return fqn; } public void setFqn(String f) { this.fqn = f; }
        public String getName() { return name; } public void setName(String n) { this.name = n; }
        public String getDescription() { return description; } public void setDescription(String d) { this.description = d; }
        public int getChildCount() { return childCount; } public void setChildCount(int c) { this.childCount = c; }
        public boolean isHasMoreChildren() { return hasMoreChildren; } public void setHasMoreChildren(boolean h) { this.hasMoreChildren = h; }
    }
    public String getAnchorFqn() { return anchorFqn; } public void setAnchorFqn(String a) { this.anchorFqn = a; }
    public String getCurrentLevel() { return currentLevel; } public void setCurrentLevel(String c) { this.currentLevel = c; }
    public List<NavNode> getChildren() { return children; } public void setChildren(List<NavNode> c) { this.children = c; }
    public boolean isHasMore() { return hasMore; } public void setHasMore(boolean h) { this.hasMore = h; }
    public String getNextCursor() { return nextCursor; } public void setNextCursor(String n) { this.nextCursor = n; }
}
