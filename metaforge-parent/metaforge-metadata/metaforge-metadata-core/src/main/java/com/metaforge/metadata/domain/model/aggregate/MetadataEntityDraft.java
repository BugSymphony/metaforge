package com.metaforge.metadata.domain.model.aggregate;

import com.metaforge.metadata.domain.model.valueobject.EntitySchemaFQN;
import com.metaforge.metadata.domain.model.valueobject.FQN;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 元数据草稿聚合根——草稿表。
 * 编辑态数据，物理隔离于主表，对外完全不可见。
 */
public class MetadataEntityDraft {

    private Long id;
    private FQN fqn;
    private String name;
    private String description;
    private String parentFqn;
    private EntitySchemaFQN entitySchemaFqn;
    private Map<String, Object> content;
    private List<Float> embedding;
    private Integer baseVersion;
    private String createdBy;
    private LocalDateTime createdTime;
    private String updatedBy;
    private LocalDateTime updatedTime;

    public MetadataEntityDraft() {}

    public MetadataEntityDraft(FQN fqn, String name, String description, String parentFqn,
                               EntitySchemaFQN entitySchemaFqn, Map<String, Object> content,
                               Integer baseVersion, String createdBy) {
        this.fqn = Objects.requireNonNull(fqn);
        this.name = Objects.requireNonNull(name);
        this.description = description;
        this.parentFqn = parentFqn;
        this.entitySchemaFqn = Objects.requireNonNull(entitySchemaFqn);
        this.content = Objects.requireNonNull(content);
        this.baseVersion = baseVersion;
        this.createdBy = Objects.requireNonNull(createdBy);
        this.createdTime = LocalDateTime.now();
        this.updatedBy = createdBy;
        this.updatedTime = this.createdTime;
    }

    /**
     * 更新草稿内容。
     */
    public void updateContent(Map<String, Object> newContent, String updatedBy) {
        this.content = Objects.requireNonNull(newContent);
        this.updatedBy = Objects.requireNonNull(updatedBy);
        this.updatedTime = LocalDateTime.now();
    }

    public void assignId(Long id) { this.id = id; }

    public void setId(Long id) { this.id = id; }
    public void setFqn(FQN fqn) { this.fqn = fqn; }
    public void setEntitySchemaFqn(EntitySchemaFQN entitySchemaFqn) { this.entitySchemaFqn = entitySchemaFqn; }
    public void setBaseVersion(Integer baseVersion) { this.baseVersion = baseVersion; }
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
    public Integer getBaseVersion() { return baseVersion; }
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
