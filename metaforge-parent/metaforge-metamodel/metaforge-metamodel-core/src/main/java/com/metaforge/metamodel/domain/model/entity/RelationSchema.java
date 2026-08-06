package com.metaforge.metamodel.domain.model.entity;

import com.metaforge.metamodel.api.enums.AssociationType;
import com.metaforge.metamodel.api.enums.Cardinality;
import com.metaforge.metamodel.domain.model.valueobject.Fqn;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * RelationSchema 领域实体。
 * 核心语义层一等元素——实体间关联建模定义。
 */
public class RelationSchema {

    private Long id;
    private Fqn fqn;
    private String packageFqn;
    private String bundleVersionFqn;
    private String name;
    private String description;
    private String sourceFqn;
    private String targetFqn;
    private AssociationType associationType;
    private Cardinality cardinalitySource;
    private Cardinality cardinalityTarget;
    private String nativeAttributes;
    private String mountedTemplateFqns;
    private String jsonSchema;
    private String embedding;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public RelationSchema() {}

    public static RelationSchema create(Fqn fqn, String packageFqn, String bundleVersionFqn,
                                         String name, String description,
                                         String sourceFqn, String targetFqn,
                                         AssociationType associationType,
                                         Cardinality cardinalitySource,
                                         Cardinality cardinalityTarget) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("关系 name 不能为空");
        if (description == null || description.isBlank()) throw new IllegalArgumentException("关系 description 不能为空");
        EntitySchema.validateSegment(fqn);
        RelationSchema entity = new RelationSchema();
        entity.fqn = fqn;
        entity.packageFqn = packageFqn;
        entity.bundleVersionFqn = bundleVersionFqn;
        entity.name = name;
        entity.description = description;
        entity.sourceFqn = sourceFqn;
        entity.targetFqn = targetFqn;
        entity.associationType = associationType;
        entity.cardinalitySource = cardinalitySource;
        entity.cardinalityTarget = cardinalityTarget;
        entity.createdTime = LocalDateTime.now();
        entity.updatedTime = LocalDateTime.now();
        return entity;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Fqn getFqn() { return fqn; }
    public void setFqn(Fqn fqn) { this.fqn = fqn; }
    public String getFqnValue() { return fqn != null ? fqn.getValue() : null; }
    public String getPackageFqn() { return packageFqn; }
    public void setPackageFqn(String packageFqn) { this.packageFqn = packageFqn; }
    public String getBundleVersionFqn() { return bundleVersionFqn; }
    public void setBundleVersionFqn(String bundleVersionFqn) { this.bundleVersionFqn = bundleVersionFqn; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSourceFqn() { return sourceFqn; }
    public void setSourceFqn(String sourceFqn) { this.sourceFqn = sourceFqn; }
    public String getTargetFqn() { return targetFqn; }
    public void setTargetFqn(String targetFqn) { this.targetFqn = targetFqn; }
    public AssociationType getAssociationType() { return associationType; }
    public void setAssociationType(AssociationType associationType) { this.associationType = associationType; }
    public Cardinality getCardinalitySource() { return cardinalitySource; }
    public void setCardinalitySource(Cardinality cardinalitySource) { this.cardinalitySource = cardinalitySource; }
    public Cardinality getCardinalityTarget() { return cardinalityTarget; }
    public void setCardinalityTarget(Cardinality cardinalityTarget) { this.cardinalityTarget = cardinalityTarget; }
    public String getNativeAttributes() { return nativeAttributes; }
    public void setNativeAttributes(String nativeAttributes) { this.nativeAttributes = nativeAttributes; }
    public String getMountedTemplateFqns() { return mountedTemplateFqns; }
    public void setMountedTemplateFqns(String mountedTemplateFqns) { this.mountedTemplateFqns = mountedTemplateFqns; }
    public String getJsonSchema() { return jsonSchema; }
    public void setJsonSchema(String jsonSchema) { this.jsonSchema = jsonSchema; }
    public String getEmbedding() { return embedding; }
    public void setEmbedding(String embedding) { this.embedding = embedding; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RelationSchema that)) return false;
        return Objects.equals(fqn, that.fqn);
    }

    @Override
    public int hashCode() { return Objects.hash(fqn); }
}
