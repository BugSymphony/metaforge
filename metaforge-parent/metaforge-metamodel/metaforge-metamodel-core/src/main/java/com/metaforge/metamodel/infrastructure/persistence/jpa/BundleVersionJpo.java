package com.metaforge.metamodel.infrastructure.persistence.jpa;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * BundleVersion JPA 持久化实体（JPO）。
 * 映射 metamodel_governance.bundle_version 表。
 */
@Entity
@Table(name = "bundle_version", schema = "metamodel_governance")
public class BundleVersionJpo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fqn", nullable = false, unique = true, length = 512)
    private String fqn;

    @Column(name = "bundle_fqn", nullable = false, length = 512)
    private String bundleFqn;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "DRAFT";

    @Column(name = "source_version_fqn", length = 512)
    private String sourceVersionFqn;

    @Column(name = "upgrade_level", length = 20)
    private String upgradeLevel;

    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;

    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdTime == null) {
            createdTime = now;
        }
        if (updatedTime == null) {
            updatedTime = now;
        }
        if (status == null) {
            status = "DRAFT";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedTime = LocalDateTime.now();
    }

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
