package com.metaforge.graph.domain.model.aggregate;

import com.metaforge.graph.domain.model.valueobject.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 生效态关系聚合根。
 * 对应主表 relation_instance 的领域模型。
 */
public class RelationInstance {

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
    private VersionNumber currentVersion;
    private String createdBy;
    private LocalDateTime createdTime;
    private String updatedBy;
    private LocalDateTime updatedTime;

    public RelationInstance() {}

    public static RelationInstance fromDraft(RelationInstanceDraft draft) {
        RelationInstance instance = new RelationInstance();
        instance.fqn = draft.getFqn();
        instance.name = draft.getName();
        instance.description = draft.getDescription();
        instance.sourceEntityFqn = draft.getSourceEntityFqn();
        instance.targetEntityFqn = draft.getTargetEntityFqn();
        instance.relationType = draft.getRelationType();
        instance.relationSchemaFqn = draft.getRelationSchemaFqn();
        instance.content = draft.getContent();
        instance.embedding = draft.getEmbedding();
        instance.currentVersion = VersionNumber.initial();
        instance.createdBy = draft.getCreatedBy();
        instance.updatedBy = draft.getCreatedBy();
        return instance;
    }

    public void deprecate() {
        // 标记下线业务逻辑由应用服务层统一处理
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

    public VersionNumber getCurrentVersion() { return currentVersion; }
    public void setCurrentVersion(VersionNumber currentVersion) { this.currentVersion = currentVersion; }

    public Integer getCurrentVersionValue() { return currentVersion != null ? currentVersion.getValue() : null; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
}
