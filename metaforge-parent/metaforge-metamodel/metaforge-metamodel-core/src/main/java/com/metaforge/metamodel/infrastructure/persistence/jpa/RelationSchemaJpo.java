package com.metaforge.metamodel.infrastructure.persistence.jpa;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


@Entity
@Table(name = "relation_schema", schema = "metamodel_governance")
public class RelationSchemaJpo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fqn", nullable = false, unique = true, length = 512)
    private String fqn;

    @Column(name = "package_fqn", nullable = false, length = 512)
    private String packageFqn;

    @Column(name = "bundle_version_fqn", nullable = false, length = 512)
    private String bundleVersionFqn;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "source_fqn", nullable = false, length = 512)
    private String sourceFqn;

    @Column(name = "target_fqn", nullable = false, length = 512)
    private String targetFqn;

    @Column(name = "association_type", nullable = false, length = 50)
    private String associationType;

    @Column(name = "cardinality_source", nullable = false, length = 20)
    private String cardinalitySource;

    @Column(name = "cardinality_target", nullable = false, length = 20)
    private String cardinalityTarget;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "native_attributes", columnDefinition = "jsonb")
    private String nativeAttributes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "mounted_template_fqns", columnDefinition = "jsonb")
    private String mountedTemplateFqns;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "json_schema", columnDefinition = "jsonb")
    private String jsonSchema;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "embedding", columnDefinition = "jsonb")
    private String embedding;

    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;

    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdTime == null) createdTime = now;
        if (updatedTime == null) updatedTime = now;
    }

    @PreUpdate
    protected void onUpdate() { updatedTime = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFqn() { return fqn; }
    public void setFqn(String fqn) { this.fqn = fqn; }
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
    public String getAssociationType() { return associationType; }
    public void setAssociationType(String associationType) { this.associationType = associationType; }
    public String getCardinalitySource() { return cardinalitySource; }
    public void setCardinalitySource(String cardinalitySource) { this.cardinalitySource = cardinalitySource; }
    public String getCardinalityTarget() { return cardinalityTarget; }
    public void setCardinalityTarget(String cardinalityTarget) { this.cardinalityTarget = cardinalityTarget; }
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
}
