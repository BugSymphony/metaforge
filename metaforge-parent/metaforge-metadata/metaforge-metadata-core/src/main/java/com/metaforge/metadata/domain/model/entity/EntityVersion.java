package com.metaforge.metadata.domain.model.entity;

import com.metaforge.metadata.domain.model.valueobject.EntitySchemaFQN;
import com.metaforge.metadata.domain.model.valueobject.FQN;
import com.metaforge.metadata.domain.model.valueobject.VersionNumber;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 元数据历史版本领域实体——历史表。
 * 所有正式发布版本的全量快照，不可变只读归档。
 */
public class EntityVersion {

    private Long id;
    private FQN fqn;
    private String name;
    private String description;
    private String parentFqn;
    private VersionNumber version;
    private EntitySchemaFQN entitySchemaFqn;
    private Map<String, Object> content;
    private List<Float> embedding;
    private String createdBy;
    private LocalDateTime createdTime;

    public EntityVersion() {}

    public EntityVersion(FQN fqn, String name, String description, String parentFqn,
                         VersionNumber version, EntitySchemaFQN entitySchemaFqn,
                         Map<String, Object> content, String createdBy) {
        this.fqn = Objects.requireNonNull(fqn);
        this.name = Objects.requireNonNull(name);
        this.description = description;
        this.parentFqn = parentFqn;
        this.version = Objects.requireNonNull(version);
        this.entitySchemaFqn = Objects.requireNonNull(entitySchemaFqn);
        this.content = Objects.requireNonNull(content);
        this.createdBy = Objects.requireNonNull(createdBy);
        this.createdTime = LocalDateTime.now();
    }

    public void assignId(Long id) { this.id = id; }

    public void setId(Long id) { this.id = id; }
    public void setFqn(FQN fqn) { this.fqn = fqn; }
    public void setEntitySchemaFqn(EntitySchemaFQN entitySchemaFqn) { this.entitySchemaFqn = entitySchemaFqn; }
    public void setVersion(VersionNumber version) { this.version = version; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setParentFqn(String parentFqn) { this.parentFqn = parentFqn; }
    public void setContent(Map<String, Object> content) { this.content = content; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }

    public Long getId() { return id; }
    public FQN getFqn() { return fqn; }
    public String getFqnValue() { return fqn != null ? fqn.getValue() : null; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getParentFqn() { return parentFqn; }
    public VersionNumber getVersion() { return version; }
    public Integer getVersionValue() { return version != null ? version.getValue() : null; }
    public EntitySchemaFQN getEntitySchemaFqn() { return entitySchemaFqn; }
    public String getEntitySchemaFqnValue() { return entitySchemaFqn != null ? entitySchemaFqn.getValue() : null; }
    public Map<String, Object> getContent() { return content; }
    public List<Float> getEmbedding() { return embedding; }
    public void setEmbedding(List<Float> embedding) { this.embedding = embedding; }
    public String getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedTime() { return createdTime; }
}
