package com.metaforge.metadata.api.dto.response;

import com.metaforge.metadata.api.enums.ExportFormat;

public class ExportResultDto {
    private ExportFormat format;
    private String content;
    private int entityCount;

    public ExportResultDto() {}

    public ExportFormat getFormat() { return format; }
    public void setFormat(ExportFormat format) { this.format = format; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getEntityCount() { return entityCount; }
    public void setEntityCount(int entityCount) { this.entityCount = entityCount; }
}
