package com.metaforge.metadata.domain.model.aggregate;

import com.metaforge.metadata.domain.model.valueobject.EntitySchemaFQN;
import com.metaforge.metadata.domain.model.valueobject.FQN;
import com.metaforge.metadata.domain.model.valueobject.VersionNumber;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 元数据实体聚合根——主表唯一生效版本。
 * 每个 FQN 最多一条记录，是对外服务的唯一数据源。
 */
public class MetadataEntity {

    private Long id;
    private FQN fqn;
    private String name;
    private String description;
    private String parentFqn;
    private EntitySchemaFQN entitySchemaFqn;
    private Map<String, Object> content;
    private List<Float> embedding;
    private VersionNumber currentVersion;
    private String createdBy;
    private LocalDateTime createdTime;
    private String updatedBy;
    private LocalDateTime updatedTime;

    public MetadataEntity() {}

    public MetadataEntity(FQN fqn, String name, String description, String parentFqn,
                          EntitySchemaFQN entitySchemaFqn, Map<String, Object> content,
                          VersionNumber currentVersion, String createdBy) {
        this.fqn = Objects.requireNonNull(fqn);
        this.name = Objects.requireNonNull(name);
        this.description = description;
        this.parentFqn = parentFqn;
        this.entitySchemaFqn = Objects.requireNonNull(entitySchemaFqn);
        this.content = Objects.requireNonNull(content);
        this.currentVersion = currentVersion != null ? currentVersion : VersionNumber.initial();
        this.createdBy = Objects.requireNonNull(createdBy);
        this.createdTime = LocalDateTime.now();
        this.updatedBy = createdBy;
        this.updatedTime = this.createdTime;
    }

    /**
     * 更新内容字段，并更新版本号和审计信息。
     */
    public void updateContent(Map<String, Object> newContent, String updatedBy) {
        this.content = Objects.requireNonNull(newContent);
        this.currentVersion = this.currentVersion.increment();
        this.updatedBy = Objects.requireNonNull(updatedBy);
        this.updatedTime = LocalDateTime.now();
    }

    /**
     * 递增版本号（用于生效时归档前）。
     */
    public void incrementVersion() {
        this.currentVersion = this.currentVersion.increment();
    }

    public void assignId(Long id) { this.id = id; }

    public void setId(Long id) { this.id = id; }
    public void setFqn(FQN fqn) { this.fqn = fqn; }
    public void setEntitySchemaFqn(EntitySchemaFQN entitySchemaFqn) { this.entitySchemaFqn = entitySchemaFqn; }
    public void setCurrentVersion(VersionNumber currentVersion) { this.currentVersion = currentVersion; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }

    public Long getId() { return id; }
    public FQN getFqn() { return fqn; }
    public String getFqnValue() { return fqn != null ? fqn.getValue() : null; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getParentFqn() { return parentFqn; }
    public EntitySchemaFQN getEntitySchemaFqn() { return entitySchemaFqn; }
    public String getEntitySchemaFqnValue() { return entitySchemaFqn != null ? entitySchemaFqn.getValue() : null; }
    public Map<String, Object> getContent() { return content; }
    public List<Float> getEmbedding() { return embedding; }
    public void setEmbedding(List<Float> embedding) { this.embedding = embedding; }
    public VersionNumber getCurrentVersion() { return currentVersion; }
    public Integer getCurrentVersionValue() { return currentVersion != null ? currentVersion.getValue() : null; }
    public String getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public String getUpdatedBy() { return updatedBy; }
    public LocalDateTime getUpdatedTime() { return updatedTime; }

    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setParentFqn(String parentFqn) { this.parentFqn = parentFqn; }
    public void setContent(Map<String, Object> content) { this.content = content; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
}
