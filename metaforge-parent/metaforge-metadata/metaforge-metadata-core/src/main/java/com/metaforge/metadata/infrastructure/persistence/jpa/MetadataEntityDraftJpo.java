package com.metaforge.metadata.infrastructure.persistence.jpa;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "metadata_entity_draft", schema = "metadata_management")
public class MetadataEntityDraftJpo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fqn", nullable = false, unique = true, length = 1024)
    private String fqn;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "parent_fqn", length = 1024)
    private String parentFqn;

    @Column(name = "entity_schema_fqn", nullable = false, length = 512)
    private String entitySchemaFqn;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content", nullable = false, columnDefinition = "jsonb")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "embedding", columnDefinition = "jsonb")
    private String embedding;

    @Column(name = "base_version")
    private Integer baseVersion;

    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;

    @Column(name = "updated_by", nullable = false, length = 255)
    private String updatedBy;

    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdTime == null) createdTime = now;
        if (updatedTime == null) updatedTime = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedTime = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getEmbedding() { return embedding; }
    public void setEmbedding(String embedding) { this.embedding = embedding; }
    public Integer getBaseVersion() { return baseVersion; }
    public void setBaseVersion(Integer baseVersion) { this.baseVersion = baseVersion; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
}
