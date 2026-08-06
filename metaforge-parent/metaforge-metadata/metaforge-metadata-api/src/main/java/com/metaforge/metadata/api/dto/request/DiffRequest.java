package com.metaforge.metadata.api.dto.request;

public class DiffRequest {
    private String fqn;
    private int versionA;
    private int versionB;

    public DiffRequest() {}

    public String getFqn() { return fqn; }
    public void setFqn(String fqn) { this.fqn = fqn; }
    public int getVersionA() { return versionA; }
    public void setVersionA(int versionA) { this.versionA = versionA; }
    public int getVersionB() { return versionB; }
    public void setVersionB(int versionB) { this.versionB = versionB; }
}
