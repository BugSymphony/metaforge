package com.metaforge.graph.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * 更新草稿内容请求。
 */
public class UpdateDraftContentRequest {

    @NotNull(message = "属性内容不能为空")
    private Map<String, Object> content;

    private List<Float> embedding;

    public UpdateDraftContentRequest() {}

    public Map<String, Object> getContent() { return content; }
    public void setContent(Map<String, Object> content) { this.content = content; }

    public List<Float> getEmbedding() { return embedding; }
    public void setEmbedding(List<Float> embedding) { this.embedding = embedding; }
}
