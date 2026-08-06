package com.metaforge.metamodel.api.dto.response;

import java.time.LocalDateTime;

/**
 * Bundle 响应 DTO。
 */
public class BundleDto {

    private String fqn;
    private String name;
    private String description;
    private String owner;
    private boolean system;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public String getFqn() { return fqn; }
    public void setFqn(String fqn) { this.fqn = fqn; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public boolean isSystem() { return system; }
    public void setSystem(boolean system) { this.system = system; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
}
