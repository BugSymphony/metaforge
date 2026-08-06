package com.metaforge.graph.api.dto;

/**
 * 版本差异对比请求 DTO。
 */
public class DiffRequest {

    private String fqn;
    private Integer versionA;
    private Integer versionB;

    public DiffRequest() {}

    public String getFqn() { return fqn; }
    public void setFqn(String fqn) { this.fqn = fqn; }

    public Integer getVersionA() { return versionA; }
    public void setVersionA(Integer versionA) { this.versionA = versionA; }

    public Integer getVersionB() { return versionB; }
    public void setVersionB(Integer versionB) { this.versionB = versionB; }
}
