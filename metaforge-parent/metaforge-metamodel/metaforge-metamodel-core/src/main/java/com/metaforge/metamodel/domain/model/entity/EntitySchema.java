package com.metaforge.metamodel.domain.model.entity;

import com.metaforge.metamodel.domain.model.valueobject.Fqn;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * EntitySchema 领域实体。
 * 核心语义层一等元素——领域概念建模定义。
 */
public class EntitySchema {

    private Long id;
    private Fqn fqn;
    private String packageFqn;
    private String bundleVersionFqn;
    private String name;
    private String description;
    private String nativeAttributes;
    private String mountedTemplateFqns;
    private String jsonSchema;
    private String embedding;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public EntitySchema() {}

    /**
     * 创建 EntitySchema，校验 FQN segment 不含保留分隔符 : 和 .
     */
    public static EntitySchema create(Fqn fqn, String packageFqn, String bundleVersionFqn,
                                       String name, String description) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("实体 name 不能为空");
        if (description == null || description.isBlank()) throw new IllegalArgumentException("实体 description 不能为空");
        validateSegment(fqn);
        EntitySchema entity = new EntitySchema();
        entity.fqn = fqn;
        entity.packageFqn = packageFqn;
        entity.bundleVersionFqn = bundleVersionFqn;
        entity.name = name;
        entity.description = description;
        entity.createdTime = LocalDateTime.now();
        entity.updatedTime = LocalDateTime.now();
        return entity;
    }

    /**
     * FQN segment 字符校验：segment 中禁止包含 : 和 .（保留分隔符）。
     */
    public static void validateSegment(Fqn fqn) {
        String value = fqn.getValue();
        if (value != null) {
            int lastDot = value.lastIndexOf('.');
            if (lastDot >= 0) {
                String shortName = value.substring(lastDot + 1);
                if (shortName.contains(":") || shortName.contains(".")) {
                    throw new IllegalArgumentException("FQN segment 不允许包含保留分隔符 : 和 .");
                }
            }
        }
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
        if (!(o instanceof EntitySchema that)) return false;
        return Objects.equals(fqn, that.fqn);
    }

    @Override
    public int hashCode() { return Objects.hash(fqn); }
}
