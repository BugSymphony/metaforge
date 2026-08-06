package com.metaforge.graph.api.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 生效态关系实例响应 DTO。
 */
public class RelationInstanceDto {

    private Long id;
    private String fqn;
    private String name;
    private String description;
    private String sourceEntityFqn;
    private String targetEntityFqn;
    private String relationType;
    private String relationSchemaFqn;
    private Map<String, Object> content;
    private List<Float> embedding;
    private Integer currentVersion;
    private String createdBy;
    private LocalDateTime createdTime;
    private String updatedBy;
    private LocalDateTime updatedTime;

    public RelationInstanceDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFqn() { return fqn; }
    public void setFqn(String fqn) { this.fqn = fqn; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSourceEntityFqn() { return sourceEntityFqn; }
    public void setSourceEntityFqn(String sourceEntityFqn) { this.sourceEntityFqn = sourceEntityFqn; }

    public String getTargetEntityFqn() { return targetEntityFqn; }
    public void setTargetEntityFqn(String targetEntityFqn) { this.targetEntityFqn = targetEntityFqn; }

    public String getRelationType() { return relationType; }
    public void setRelationType(String relationType) { this.relationType = relationType; }

    public String getRelationSchemaFqn() { return relationSchemaFqn; }
    public void setRelationSchemaFqn(String relationSchemaFqn) { this.relationSchemaFqn = relationSchemaFqn; }

    public Map<String, Object> getContent() { return content; }
    public void setContent(Map<String, Object> content) { this.content = content; }

    public List<Float> getEmbedding() { return embedding; }
    public void setEmbedding(List<Float> embedding) { this.embedding = embedding; }

    public Integer getCurrentVersion() { return currentVersion; }
    public void setCurrentVersion(Integer currentVersion) { this.currentVersion = currentVersion; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
}
