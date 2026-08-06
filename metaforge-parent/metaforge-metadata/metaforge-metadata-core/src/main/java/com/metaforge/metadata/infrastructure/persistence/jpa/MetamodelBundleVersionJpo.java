package com.metaforge.metadata.infrastructure.persistence.jpa;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 上游 metamodel-governance BC 的 BundleVersion 只读视图（跨 Schema 网关）。
 * <p>用于判定 EntitySchema 所属版本是否已发布。</p>
 */
@Entity
@Table(name = "bundle_version", schema = "metamodel_governance")
public class MetamodelBundleVersionJpo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fqn", nullable = false, unique = true, length = 512)
    private String fqn;

    @Column(name = "bundle_fqn", nullable = false, length = 512)
    private String bundleFqn;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "source_version_fqn", length = 512)
    private String sourceVersionFqn;

    @Column(name = "upgrade_level", length = 20)
    private String upgradeLevel;

    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;

    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
}
