package com.metaforge.agent.cognition.core.domain.model.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class EntityProfile {

    private String fqn;
    private String name;
    private String description;
    private String entitySchemaFqn;
    private Map<String, Object> content;
    private List<NativeAttributeDetail> schemaAttributes;
    private Integer currentVersion;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    private boolean empty;

    public static class NativeAttributeDetail {
        private String name;
        private String type;
        private boolean required;
        private String description;
        private Map<String, Object> constraints;
        public String getName() { return name; } public void setName(String name) { this.name = name; }
        public String getType() { return type; } public void setType(String type) { this.type = type; }
        public boolean isRequired() { return required; } public void setRequired(boolean required) { this.required = required; }
        public String getDescription() { return description; } public void setDescription(String description) { this.description = description; }
        public Map<String, Object> getConstraints() { return constraints; } public void setConstraints(Map<String, Object> constraints) { this.constraints = constraints; }
    }
    public String getFqn() { return fqn; } public void setFqn(String fqn) { this.fqn = fqn; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getDescription() { return description; } public void setDescription(String description) { this.description = description; }
    public String getEntitySchemaFqn() { return entitySchemaFqn; } public void setEntitySchemaFqn(String entitySchemaFqn) { this.entitySchemaFqn = entitySchemaFqn; }
    public Map<String, Object> getContent() { return content; } public void setContent(Map<String, Object> content) { this.content = content; }
    public List<NativeAttributeDetail> getSchemaAttributes() { return schemaAttributes; } public void setSchemaAttributes(List<NativeAttributeDetail> schemaAttributes) { this.schemaAttributes = schemaAttributes; }
    public Integer getCurrentVersion() { return currentVersion; } public void setCurrentVersion(Integer currentVersion) { this.currentVersion = currentVersion; }
    public String getCreatedBy() { return createdBy; } public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getUpdatedBy() { return updatedBy; } public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getCreatedTime() { return createdTime; } public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
    public boolean isEmpty() { return empty; } public void setEmpty(boolean empty) { this.empty = empty; }
}