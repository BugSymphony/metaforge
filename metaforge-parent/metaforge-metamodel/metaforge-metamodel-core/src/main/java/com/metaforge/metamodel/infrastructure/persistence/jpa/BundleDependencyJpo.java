package com.metaforge.metamodel.infrastructure.persistence.jpa;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * BundleDependency JPA 持久化实体（JPO）。
 * 映射 metamodel_governance.bundle_dependency 表。
 */
@Entity
@Table(name = "bundle_dependency", schema = "metamodel_governance",
        uniqueConstraints = @UniqueConstraint(columnNames = {"source_version_fqn", "target_version_fqn"}))
public class BundleDependencyJpo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_version_fqn", nullable = false, length = 512)
    private String sourceVersionFqn;

    @Column(name = "target_version_fqn", nullable = false, length = 512)
    private String targetVersionFqn;

    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;

    @PrePersist
    protected void onCreate() {
        if (createdTime == null) {
            createdTime = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSourceVersionFqn() { return sourceVersionFqn; }
    public void setSourceVersionFqn(String sourceVersionFqn) { this.sourceVersionFqn = sourceVersionFqn; }
    public String getTargetVersionFqn() { return targetVersionFqn; }
    public void setTargetVersionFqn(String targetVersionFqn) { this.targetVersionFqn = targetVersionFqn; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
}
