package com.metaforge.graph.api.dto;

/**
 * 拓扑校验请求 DTO。
 */
public class TopologyValidationRequest {

    private String fqnPrefix;
    private String relationType;

    public TopologyValidationRequest() {}

    public String getFqnPrefix() { return fqnPrefix; }
    public void setFqnPrefix(String fqnPrefix) { this.fqnPrefix = fqnPrefix; }

    public String getRelationType() { return relationType; }
    public void setRelationType(String relationType) { this.relationType = relationType; }
}
