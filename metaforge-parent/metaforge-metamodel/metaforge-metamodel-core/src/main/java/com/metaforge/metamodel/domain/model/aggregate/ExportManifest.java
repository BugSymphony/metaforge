package com.metaforge.metamodel.domain.model.aggregate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * ExportManifest 领域聚合根。
 * 导出清单，绑定到特定 BundleVersion，声明对外可见的 Package 命名空间白名单。
 */
public class ExportManifest {

    private Long id;
    private String bundleVersionFqn;
    private List<String> exportedPackageFqns;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public ExportManifest() {}

    public static ExportManifest create(String bundleVersionFqn, List<String> exportedPackageFqns) {
        ExportManifest manifest = new ExportManifest();
        manifest.bundleVersionFqn = bundleVersionFqn;
        manifest.exportedPackageFqns = exportedPackageFqns != null
                ? Collections.unmodifiableList(exportedPackageFqns)
                : Collections.emptyList();
        manifest.createdTime = LocalDateTime.now();
        manifest.updatedTime = LocalDateTime.now();
        return manifest;
    }

    public void updateExportedPackages(List<String> packageFqns) {
        this.exportedPackageFqns = packageFqns != null
                ? Collections.unmodifiableList(packageFqns)
                : Collections.emptyList();
        this.updatedTime = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBundleVersionFqn() { return bundleVersionFqn; }
    public void setBundleVersionFqn(String bundleVersionFqn) { this.bundleVersionFqn = bundleVersionFqn; }
    public List<String> getExportedPackageFqns() { return exportedPackageFqns; }
    public void setExportedPackageFqns(List<String> exportedPackageFqns) { this.exportedPackageFqns = exportedPackageFqns; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExportManifest that)) return false;
        return Objects.equals(bundleVersionFqn, that.bundleVersionFqn);
    }

    @Override
    public int hashCode() { return Objects.hash(bundleVersionFqn); }
}
