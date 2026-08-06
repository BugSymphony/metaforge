package com.metaforge.metadata.api.dto.request;

import com.metaforge.metadata.api.enums.ImportFormat;
import com.metaforge.metadata.api.enums.ImportStrategy;

public class ImportRequest {
    private String content;
    private ImportFormat format;
    private ImportStrategy strategy;
    private String createdBy;

    public ImportRequest() {}

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public ImportFormat getFormat() { return format; }
    public void setFormat(ImportFormat format) { this.format = format; }
    public ImportStrategy getStrategy() { return strategy; }
    public void setStrategy(ImportStrategy strategy) { this.strategy = strategy; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
