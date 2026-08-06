package com.metaforge.graph.api.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 历史版本响应 DTO。
 */
public class RelationVersionDto {

    private Long id;
    private String fqn;
    private String name;
    private String description;
    private String sourceEntityFqn;
    private String targetEntityFqn;
    private String relationType;
    private String relationSchemaFqn;
    private Map<String, Object> content;
    private java.util.List<Float> embedding;
    private Integer version;
    private String activatedBy;
    private LocalDateTime activatedTime;

    public RelationVersionDto() {}

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

    public java.util.List<Float> getEmbedding() { return embedding; }
    public void setEmbedding(java.util.List<Float> embedding) { this.embedding = embedding; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public String getActivatedBy() { return activatedBy; }
    public void setActivatedBy(String activatedBy) { this.activatedBy = activatedBy; }

    public LocalDateTime getActivatedTime() { return activatedTime; }
    public void setActivatedTime(LocalDateTime activatedTime) { this.activatedTime = activatedTime; }
}
