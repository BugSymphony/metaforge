package com.metaforge.graph.domain.model.aggregate;

import com.metaforge.graph.domain.model.valueobject.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 草稿态关系聚合根。
 * 对应草稿表 relation_instance_draft 的领域模型。
 */
public class RelationInstanceDraft {

    private Long id;
    private FQN fqn;
    private RelationName name;
    private RelationDescription description;
    private EntityFQN sourceEntityFqn;
    private EntityFQN targetEntityFqn;
    private String relationType;
    private RelationSchemaFQN relationSchemaFqn;
    private Map<String, Object> content;
    private List<Float> embedding;
    private Integer baseVersion;
    private String createdBy;
    private LocalDateTime createdTime;
    private String updatedBy;
    private LocalDateTime updatedTime;

    public RelationInstanceDraft() {}

    /**
     * 更新内容并实时校验（校验逻辑由应用层服务调用领域服务执行）。
     */
    public void updateContent(Map<String, Object> newContent, List<Float> newEmbedding) {
        this.content = newContent;
        if (newEmbedding != null) {
            this.embedding = newEmbedding;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public FQN getFqn() { return fqn; }
    public void setFqn(FQN fqn) { this.fqn = fqn; }

    public String getFqnValue() { return fqn != null ? fqn.getValue() : null; }

    public RelationName getName() { return name; }
    public void setName(RelationName name) { this.name = name; }

    public String getNameValue() { return name != null ? name.getValue() : null; }

    public RelationDescription getDescription() { return description; }
    public void setDescription(RelationDescription description) { this.description = description; }

    public String getDescriptionValue() { return description != null ? description.getValue() : null; }

    public EntityFQN getSourceEntityFqn() { return sourceEntityFqn; }
    public void setSourceEntityFqn(EntityFQN sourceEntityFqn) { this.sourceEntityFqn = sourceEntityFqn; }

    public String getSourceEntityFqnValue() { return sourceEntityFqn != null ? sourceEntityFqn.getValue() : null; }

    public EntityFQN getTargetEntityFqn() { return targetEntityFqn; }
    public void setTargetEntityFqn(EntityFQN targetEntityFqn) { this.targetEntityFqn = targetEntityFqn; }

    public String getTargetEntityFqnValue() { return targetEntityFqn != null ? targetEntityFqn.getValue() : null; }

    public String getRelationType() { return relationType; }
    public void setRelationType(String relationType) { this.relationType = relationType; }

    public RelationSchemaFQN getRelationSchemaFqn() { return relationSchemaFqn; }
    public void setRelationSchemaFqn(RelationSchemaFQN relationSchemaFqn) { this.relationSchemaFqn = relationSchemaFqn; }

    public String getRelationSchemaFqnValue() { return relationSchemaFqn != null ? relationSchemaFqn.getValue() : null; }

    public Map<String, Object> getContent() { return content; }
    public void setContent(Map<String, Object> content) { this.content = content; }

    public List<Float> getEmbedding() { return embedding; }
    public void setEmbedding(List<Float> embedding) { this.embedding = embedding; }

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
