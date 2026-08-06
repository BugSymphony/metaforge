package com.metaforge.metadata.api.dto.request;

import com.metaforge.common.dto.PageRequest;
import java.util.List;

public class MetadataQueryRequest {
    private String fqn;
    private List<String> fqnPrefixes;
    private String entitySchemaFqn;
    private PageRequest pageRequest;

    public MetadataQueryRequest() {}

    public String getFqn() { return fqn; }
    public void setFqn(String fqn) { this.fqn = fqn; }
    public List<String> getFqnPrefixes() { return fqnPrefixes; }
    public void setFqnPrefixes(List<String> fqnPrefixes) { this.fqnPrefixes = fqnPrefixes; }
    public String getEntitySchemaFqn() { return entitySchemaFqn; }
    public void setEntitySchemaFqn(String entitySchemaFqn) { this.entitySchemaFqn = entitySchemaFqn; }
    public PageRequest getPageRequest() { return pageRequest; }
    public void setPageRequest(PageRequest pageRequest) { this.pageRequest = pageRequest; }
}
