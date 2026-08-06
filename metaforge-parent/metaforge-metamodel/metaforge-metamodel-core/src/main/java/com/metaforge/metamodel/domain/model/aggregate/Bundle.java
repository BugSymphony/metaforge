package com.metaforge.metamodel.domain.model.aggregate;

import com.metaforge.metamodel.domain.model.valueobject.BundleCode;
import com.metaforge.metamodel.domain.model.valueobject.Fqn;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Bundle 聚合根。
 * 顶层治理单元，管理 Bundle 元信息。
 */
public class Bundle {

    private Long id;
    private Fqn fqn;
    private String name;
    private String description;
    private String owner;
    private boolean system;
    private String embedding;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public Bundle() {}

    /**
     * 创建 Bundle。
     *
     * @param bundleCode  Bundle Code 值对象（需通过格式校验）
     * @param name        Bundle 显示名
     * @param description Bundle 描述（必填）
     * @param owner       负责人
     * @throws IllegalArgumentException 如果描述为空或 code 格式不合法
     */
    public static Bundle create(BundleCode bundleCode, String name,
                                 String description, String owner) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Bundle 描述不能为空");
        }
        Bundle bundle = new Bundle();
        bundle.fqn = Fqn.of(bundleCode.getValue());
        bundle.name = name;
        bundle.description = description;
        bundle.owner = owner;
        bundle.system = false;
        bundle.createdTime = LocalDateTime.now();
        bundle.updatedTime = LocalDateTime.now();
        return bundle;
    }

    /**
     * 标记为系统内置 Bundle（仅限 metaforge 等预置 Bundle 初始化使用）。
     */
    public void markAsSystem() {
        this.system = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Fqn getFqn() { return fqn; }
    public void setFqn(Fqn fqn) { this.fqn = fqn; }
    public String getFqnValue() { return fqn != null ? fqn.getValue() : null; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public boolean isSystem() { return system; }
    public void setSystem(boolean system) { this.system = system; }
    public String getEmbedding() { return embedding; }
    public void setEmbedding(String embedding) { this.embedding = embedding; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Bundle bundle)) return false;
        return Objects.equals(fqn, bundle.fqn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fqn);
    }
}
