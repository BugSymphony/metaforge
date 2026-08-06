package com.metaforge.metamodel.api.dto.request;

public class ExportRequest {

    /** 导出格式: YAML 或 JSON */
    private String format = "JSON";

    /** Bundle FQN（Bundle 级导出） */
    private String bundleFqn;

    /** Package-level scope（Package 级导出，可选） */
    private String packageFqn;

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public String getBundleFqn() { return bundleFqn; }
    public void setBundleFqn(String bundleFqn) { this.bundleFqn = bundleFqn; }
    public String getPackageFqn() { return packageFqn; }
    public void setPackageFqn(String packageFqn) { this.packageFqn = packageFqn; }
}
