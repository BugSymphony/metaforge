package com.metaforge.metamodel.domain.model.entity;

import com.metaforge.metamodel.domain.model.valueobject.Fqn;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * AttributeTemplate 领域实体。
 * 属性定义层辅助复用单元，全局归属 Bundle 版本级。
 */
public class AttributeTemplate {

    private Long id;
    private Fqn fqn;
    private String bundleVersionFqn;
    private String name;
    private String description;
    private String attributeDefinitions;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public AttributeTemplate() {}

    public static AttributeTemplate create(Fqn fqn, String bundleVersionFqn,
                                            String name, String description) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("模板 name 不能为空");
        EntitySchema.validateSegment(fqn);
        AttributeTemplate entity = new AttributeTemplate();
        entity.fqn = fqn;
        entity.bundleVersionFqn = bundleVersionFqn;
        entity.name = name;
        entity.description = description;
        entity.createdTime = LocalDateTime.now();
        entity.updatedTime = LocalDateTime.now();
        return entity;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Fqn getFqn() { return fqn; }
    public void setFqn(Fqn fqn) { this.fqn = fqn; }
    public String getFqnValue() { return fqn != null ? fqn.getValue() : null; }
    public String getBundleVersionFqn() { return bundleVersionFqn; }
    public void setBundleVersionFqn(String bundleVersionFqn) { this.bundleVersionFqn = bundleVersionFqn; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAttributeDefinitions() { return attributeDefinitions; }
    public void setAttributeDefinitions(String attributeDefinitions) { this.attributeDefinitions = attributeDefinitions; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AttributeTemplate that)) return false;
        return Objects.equals(fqn, that.fqn);
    }

    @Override
    public int hashCode() { return Objects.hash(fqn); }
}
