package com.metaforge.metamodel.api.dto.request;

/**
 * 创建 Package 请求 DTO。
 */
public class CreatePackageRequest {

    /** 所属 BundleVersion FQN */
    private String bundleVersionFqn;

    /** 父 Package FQN（null 表示根层） */
    private String parentPackageFqn;

    /** Package 短名（segment） */
    private String segment;

    /** 描述（必填） */
    private String description;

    public String getBundleVersionFqn() { return bundleVersionFqn; }
    public void setBundleVersionFqn(String bundleVersionFqn) { this.bundleVersionFqn = bundleVersionFqn; }
    public String getParentPackageFqn() { return parentPackageFqn; }
    public void setParentPackageFqn(String parentPackageFqn) { this.parentPackageFqn = parentPackageFqn; }
    public String getSegment() { return segment; }
    public void setSegment(String segment) { this.segment = segment; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
