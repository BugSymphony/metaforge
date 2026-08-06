package com.metaforge.graph.api.dto;

/**
 * 导入请求 DTO。
 */
public class ImportRequest {

    private String content;
    private String format;
    private String strategy;

    public ImportRequest() {}

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }
}
