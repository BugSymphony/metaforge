package com.metaforge.metamodel.api.dto.response;

import java.time.LocalDateTime;

public class AttributeTemplateDto {

    private String fqn;
    private String bundleVersionFqn;
    private String name;
    private String description;
    private String attributeDefinitions;
    private boolean enabled;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public String getFqn() { return fqn; }
    public void setFqn(String fqn) { this.fqn = fqn; }
    public String getBundleVersionFqn() { return bundleVersionFqn; }
    public void setBundleVersionFqn(String bundleVersionFqn) { this.bundleVersionFqn = bundleVersionFqn; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAttributeDefinitions() { return attributeDefinitions; }
    public void setAttributeDefinitions(String attributeDefinitions) { this.attributeDefinitions = attributeDefinitions; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
}
