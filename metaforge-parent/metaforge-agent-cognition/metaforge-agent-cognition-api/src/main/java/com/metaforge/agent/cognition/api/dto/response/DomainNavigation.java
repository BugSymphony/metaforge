package com.metaforge.agent.cognition.api.dto.response;

import java.util.List;

public class DomainNavigation {

    private String anchorFqn;
    private String currentLevel;
    private List<NavNode> children;
    private boolean hasMore;
    private String nextCursor;

    public String getAnchorFqn() { return anchorFqn; }
    public void setAnchorFqn(String anchorFqn) { this.anchorFqn = anchorFqn; }
    public String getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(String currentLevel) { this.currentLevel = currentLevel; }
    public List<NavNode> getChildren() { return children; }
    public void setChildren(List<NavNode> children) { this.children = children; }
    public boolean isHasMore() { return hasMore; }
    public void setHasMore(boolean hasMore) { this.hasMore = hasMore; }
    public String getNextCursor() { return nextCursor; }
    public void setNextCursor(String nextCursor) { this.nextCursor = nextCursor; }

    public static class NavNode {
        private String fqn;
        private String name;
        private String description;
        private int childCount;
        private boolean hasMoreChildren;

        public String getFqn() { return fqn; }
        public void setFqn(String fqn) { this.fqn = fqn; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public int getChildCount() { return childCount; }
        public void setChildCount(int childCount) { this.childCount = childCount; }
        public boolean isHasMoreChildren() { return hasMoreChildren; }
        public void setHasMoreChildren(boolean hasMoreChildren) { this.hasMoreChildren = hasMoreChildren; }
    }
}
