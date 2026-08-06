package com.metaforge.metadata.api.dto.request;

import com.metaforge.metadata.api.enums.ExportFormat;
import java.util.List;

public class ExportRequest {
    private List<String> fqnPrefixes;
    private String entitySchemaFqn;
    private List<String> fqns;
    private ExportFormat format;

    public ExportRequest() {}

    public List<String> getFqnPrefixes() { return fqnPrefixes; }
    public void setFqnPrefixes(List<String> fqnPrefixes) { this.fqnPrefixes = fqnPrefixes; }
    public String getEntitySchemaFqn() { return entitySchemaFqn; }
    public void setEntitySchemaFqn(String entitySchemaFqn) { this.entitySchemaFqn = entitySchemaFqn; }
    public List<String> getFqns() { return fqns; }
    public void setFqns(List<String> fqns) { this.fqns = fqns; }
    public ExportFormat getFormat() { return format; }
    public void setFormat(ExportFormat format) { this.format = format; }
}
