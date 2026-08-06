package com.metaforge.metamodel.api.dto.response;

import java.time.LocalDateTime;

/**
 * BundleVersion 响应 DTO。
 */
public class BundleVersionDto {

    private String fqn;
    private String bundleFqn;
    private String status;
    private String sourceVersionFqn;
    private String upgradeLevel;
    private boolean enabled;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public String getFqn() { return fqn; }
    public void setFqn(String fqn) { this.fqn = fqn; }
    public String getBundleFqn() { return bundleFqn; }
    public void setBundleFqn(String bundleFqn) { this.bundleFqn = bundleFqn; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSourceVersionFqn() { return sourceVersionFqn; }
    public void setSourceVersionFqn(String sourceVersionFqn) { this.sourceVersionFqn = sourceVersionFqn; }
    public String getUpgradeLevel() { return upgradeLevel; }
    public void setUpgradeLevel(String upgradeLevel) { this.upgradeLevel = upgradeLevel; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
}
