package com.metaforge.metamodel.domain.model.aggregate;

import com.metaforge.metamodel.api.enums.UpgradeLevel;
import com.metaforge.metamodel.api.enums.VersionStatus;
import com.metaforge.metamodel.domain.exception.PublishedImmutableException;
import com.metaforge.metamodel.domain.exception.VersionNotDraftException;
import com.metaforge.metamodel.domain.model.valueobject.Fqn;
import com.metaforge.metamodel.domain.model.valueobject.SemanticVersion;
import com.metaforge.metamodel.domain.service.FqnGenerator;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * BundleVersion 聚合根。
 * 管理版本两态生命周期（草稿 → 已发布，不可逆）。
 */
public class BundleVersion {

    private Long id;
    private Fqn fqn;
    private String bundleFqn;
    private VersionStatus status;
    private String sourceVersionFqn;
    private UpgradeLevel upgradeLevel;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public BundleVersion() {}

    /**
     * 创建初始草稿版本（无源版本）。
     */
    public static BundleVersion createInitialDraft(FqnGenerator fqnGenerator,
                                                    String bundleCode,
                                                    SemanticVersion version) {
        BundleVersion bv = new BundleVersion();
        bv.fqn = Fqn.of(fqnGenerator.bundleVersion(bundleCode, version.toVersionString()));
        bv.bundleFqn = fqnGenerator.bundle(bundleCode);
        bv.status = VersionStatus.DRAFT;
        bv.createdTime = LocalDateTime.now();
        bv.updatedTime = LocalDateTime.now();
        return bv;
    }

    /**
     * 从源版本（已发布版本）创建新草稿。
     * 新版本号根据源版本 + upgradeLevel 自动计算。
     */
    public static BundleVersion createDraftFrom(FqnGenerator fqnGenerator,
                                                 BundleVersion sourceVersion,
                                                 UpgradeLevel upgradeLevel) {
        if (sourceVersion.getStatus() != VersionStatus.PUBLISHED) {
            throw new VersionNotDraftException(sourceVersion.getFqnValue(),
                    sourceVersion.getStatus().name());
        }
        SemanticVersion sourceSemVer = SemanticVersion.parse(
                fqnGenerator.toVersion(sourceVersion.getFqnValue()));
        SemanticVersion newVersion = sourceSemVer.bump(upgradeLevel);

        BundleVersion bv = new BundleVersion();
        bv.fqn = Fqn.of(fqnGenerator.bundleVersion(
                fqnGenerator.toBundleCode(sourceVersion.getFqnValue()),
                newVersion.toVersionString()));
        bv.bundleFqn = sourceVersion.getBundleFqn();
        bv.status = VersionStatus.DRAFT;
        bv.sourceVersionFqn = sourceVersion.getFqnValue();
        bv.upgradeLevel = upgradeLevel;
        bv.createdTime = LocalDateTime.now();
        bv.updatedTime = LocalDateTime.now();
        return bv;
    }

    /**
     * 发布当前草稿版本。
     * 状态从 DRAFT 变为 PUBLISHED，不可逆。
     *
     * @throws VersionNotDraftException 如果当前状态不是 DRAFT
     */
    public void publish() {
        if (status != VersionStatus.DRAFT) {
            throw new VersionNotDraftException(fqn.getValue(), status.name());
        }
        status = VersionStatus.PUBLISHED;
        updatedTime = LocalDateTime.now();
    }

    /**
     * 检查当前版本是否为草稿态，非草稿则抛异常。
     */
    public void requireDraft() {
        if (status != VersionStatus.DRAFT) {
            throw new PublishedImmutableException(fqn.getValue());
        }
    }

    /**
     * 检查当前版本是否为已发布态。
     */
    public boolean isPublished() {
        return status == VersionStatus.PUBLISHED;
    }

    /**
     * 从版本状态推导元素的 enabled 状态：
     * DRAFT → false，PUBLISHED → true。
     */
    public boolean deriveEnabled() {
        return status == VersionStatus.PUBLISHED;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Fqn getFqn() { return fqn; }
    public void setFqn(Fqn fqn) { this.fqn = fqn; }
    public String getFqnValue() { return fqn != null ? fqn.getValue() : null; }
    public String getBundleFqn() { return bundleFqn; }
    public void setBundleFqn(String bundleFqn) { this.bundleFqn = bundleFqn; }
    public VersionStatus getStatus() { return status; }
    public void setStatus(VersionStatus status) { this.status = status; }
    public String getSourceVersionFqn() { return sourceVersionFqn; }
    public void setSourceVersionFqn(String sourceVersionFqn) { this.sourceVersionFqn = sourceVersionFqn; }
    public UpgradeLevel getUpgradeLevel() { return upgradeLevel; }
    public void setUpgradeLevel(UpgradeLevel upgradeLevel) { this.upgradeLevel = upgradeLevel; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BundleVersion that)) return false;
        return Objects.equals(fqn, that.fqn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fqn);
    }
}
