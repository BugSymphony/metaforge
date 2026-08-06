package com.metaforge.metadata.infrastructure.persistence.jpa;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "entity_version", schema = "metadata_management")
public class EntityVersionJpo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fqn", nullable = false, length = 1024)
    private String fqn;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "parent_fqn", length = 1024)
    private String parentFqn;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "entity_schema_fqn", nullable = false, length = 512)
    private String entitySchemaFqn;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content", nullable = false, columnDefinition = "jsonb")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "embedding", columnDefinition = "jsonb")
    private String embedding;

    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;

    @PrePersist
    protected void onCreate() {
        if (createdTime == null) createdTime = LocalDateTime.now();
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
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getEntitySchemaFqn() { return entitySchemaFqn; }
    public void setEntitySchemaFqn(String entitySchemaFqn) { this.entitySchemaFqn = entitySchemaFqn; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getEmbedding() { return embedding; }
    public void setEmbedding(String embedding) { this.embedding = embedding; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
}
