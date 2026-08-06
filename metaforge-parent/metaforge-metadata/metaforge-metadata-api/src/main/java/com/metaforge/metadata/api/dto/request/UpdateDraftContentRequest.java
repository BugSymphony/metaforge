package com.metaforge.metadata.api.dto.request;

import java.util.Map;

public class UpdateDraftContentRequest {
    private Map<String, Object> content;
    private String updatedBy;

    public UpdateDraftContentRequest() {}

    public Map<String, Object> getContent() { return content; }
    public void setContent(Map<String, Object> content) { this.content = content; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
