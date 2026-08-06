package com.metaforge.metadata.api.dto.request;

import java.util.Map;

public class CreateDraftRequest {
    private String fqn;
    private String name;
    private String description;
    private String parentFqn;
    private String entitySchemaFqn;
    private Map<String, Object> content;
    private String createdBy;

    public CreateDraftRequest() {}

    public String getFqn() { return fqn; }
    public void setFqn(String fqn) { this.fqn = fqn; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getParentFqn() { return parentFqn; }
    public void setParentFqn(String parentFqn) { this.parentFqn = parentFqn; }
    public String getEntitySchemaFqn() { return entitySchemaFqn; }
    public void setEntitySchemaFqn(String entitySchemaFqn) { this.entitySchemaFqn = entitySchemaFqn; }
    public Map<String, Object> getContent() { return content; }
    public void setContent(Map<String, Object> content) { this.content = content; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
