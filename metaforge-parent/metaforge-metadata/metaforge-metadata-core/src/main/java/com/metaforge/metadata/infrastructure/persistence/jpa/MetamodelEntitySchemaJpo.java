package com.metaforge.metadata.infrastructure.persistence.jpa;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 上游 metamodel-governance BC 的 EntitySchema 只读视图（跨 Schema 网关）。
 * <p>用于结构校验：读取已发布 EntitySchema 的 json_schema，供 metadata BC 做 JSON Schema 校验。</p>
 */
@Entity
@Table(name = "entity_schema", schema = "metamodel_governance")
public class MetamodelEntitySchemaJpo {

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
