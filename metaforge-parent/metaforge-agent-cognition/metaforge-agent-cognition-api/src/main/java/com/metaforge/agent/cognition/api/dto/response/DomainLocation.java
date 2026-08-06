package com.metaforge.agent.cognition.api.dto.response;

import java.util.List;

public class DomainLocation {

    private String entityFqn;
    private List<LocationNode> path;
    private boolean complete;
    private String note;

    public String getEntityFqn() { return entityFqn; }
    public void setEntityFqn(String entityFqn) { this.entityFqn = entityFqn; }
    public List<LocationNode> getPath() { return path; }
    public void setPath(List<LocationNode> path) { this.path = path; }
    public boolean isComplete() { return complete; }
    public void setComplete(boolean complete) { this.complete = complete; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public static class LocationNode {
        private String fqn;
        private String name;
        private String description;
        private String entitySchemaFqn;
        private int depth;

        public String getFqn() { return fqn; }
        public void setFqn(String fqn) { this.fqn = fqn; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getEntitySchemaFqn() { return entitySchemaFqn; }
        public void setEntitySchemaFqn(String entitySchemaFqn) { this.entitySchemaFqn = entitySchemaFqn; }
        public int getDepth() { return depth; }
        public void setDepth(int depth) { this.depth = depth; }
    }
}
