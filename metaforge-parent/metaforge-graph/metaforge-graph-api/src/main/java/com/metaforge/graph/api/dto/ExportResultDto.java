package com.metaforge.graph.api.dto;

/**
 * 导出结果 DTO。
 */
public class ExportResultDto {

    private int totalCount;
    private String content;
    private String format;

    public ExportResultDto() {}

    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
}
