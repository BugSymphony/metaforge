package com.metaforge.metamodel.api.dto.response;

import java.time.LocalDateTime;

/**
 * Package 响应 DTO。
 */
public class PackageDto {

    private String fqn;
    private String bundleVersionFqn;
    private String parentPackageFqn;
    private String description;
    private int depth;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public String getFqn() { return fqn; }
    public void setFqn(String fqn) { this.fqn = fqn; }
    public String getBundleVersionFqn() { return bundleVersionFqn; }
    public void setBundleVersionFqn(String bundleVersionFqn) { this.bundleVersionFqn = bundleVersionFqn; }
    public String getParentPackageFqn() { return parentPackageFqn; }
    public void setParentPackageFqn(String parentPackageFqn) { this.parentPackageFqn = parentPackageFqn; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getDepth() { return depth; }
    public void setDepth(int depth) { this.depth = depth; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
}
