package com.metaforge.metamodel.api.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public class ExportManifestDto {

    private String bundleVersionFqn;
    private List<String> exportedPackageFqns;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public String getBundleVersionFqn() { return bundleVersionFqn; }
    public void setBundleVersionFqn(String bundleVersionFqn) { this.bundleVersionFqn = bundleVersionFqn; }
    public List<String> getExportedPackageFqns() { return exportedPackageFqns; }
    public void setExportedPackageFqns(List<String> exportedPackageFqns) { this.exportedPackageFqns = exportedPackageFqns; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
}
