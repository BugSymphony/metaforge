package com.metaforge.metamodel.domain.model.entity;

import com.metaforge.metamodel.domain.exception.PackageDepthExceededException;
import com.metaforge.metamodel.domain.model.valueobject.Fqn;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Package 领域实体。
 * Bundle 版本内的纯分类容器，嵌套深度上限 5 层（根层深度 0 + 4 级子层）。
 */
public class Package {

    private Long id;
    private Fqn fqn;
    private String bundleVersionFqn;
    private String parentPackageFqn;
    private String description;
    private int depth;
    private String embedding;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public Package() {}

    /**
     * 创建 Package 并校验嵌套深度。
     *
     * @param fqn              Package FQN
     * @param bundleVersionFqn 所属 BundleVersion FQN
     * @param parentPackageFqn 父 Package FQN（根层为 null）
     * @param description      描述
     * @param parentDepth      父 Package 深度（根层传入 -1）
     * @param maxDepth         Package 最大嵌套深度上限
     * @return Package 实例
     * @throws PackageDepthExceededException 如果深度超限
     */
    public static Package create(Fqn fqn, String bundleVersionFqn, String parentPackageFqn,
                                  String description, int parentDepth, int maxDepth) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Package 描述不能为空");
        }
        int newDepth = parentDepth + 1;
        if (newDepth >= maxDepth) {
            throw new PackageDepthExceededException(newDepth + 1, maxDepth);
        }
        Package pkg = new Package();
        pkg.fqn = fqn;
        pkg.bundleVersionFqn = bundleVersionFqn;
        pkg.parentPackageFqn = parentPackageFqn;
        pkg.description = description;
        pkg.depth = newDepth;
        pkg.createdTime = LocalDateTime.now();
        pkg.updatedTime = LocalDateTime.now();
        return pkg;
    }

    public boolean isRootLevel() {
        return parentPackageFqn == null;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Fqn getFqn() { return fqn; }
    public void setFqn(Fqn fqn) { this.fqn = fqn; }
    public String getFqnValue() { return fqn != null ? fqn.getValue() : null; }
    public String getBundleVersionFqn() { return bundleVersionFqn; }
    public void setBundleVersionFqn(String bundleVersionFqn) { this.bundleVersionFqn = bundleVersionFqn; }
    public String getParentPackageFqn() { return parentPackageFqn; }
    public void setParentPackageFqn(String parentPackageFqn) { this.parentPackageFqn = parentPackageFqn; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getDepth() { return depth; }
    public void setDepth(int depth) { this.depth = depth; }
    public String getEmbedding() { return embedding; }
    public void setEmbedding(String embedding) { this.embedding = embedding; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Package pkg)) return false;
        return Objects.equals(fqn, pkg.fqn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fqn);
    }
}
