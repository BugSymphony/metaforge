package com.metaforge.metadata.api.dto.request;

import com.metaforge.common.dto.PageRequest;
import com.metaforge.metadata.api.enums.MetadataStatus;

import java.util.List;

public class AdminQueryRequest {
    private String fqn;
    private MetadataStatus status;
    private List<String> statuses;
    private PageRequest pageRequest;

    public AdminQueryRequest() {}

    public String getFqn() { return fqn; }
    public void setFqn(String fqn) { this.fqn = fqn; }
    public MetadataStatus getStatus() { return status; }
    public void setStatus(MetadataStatus status) { this.status = status; }
    public List<String> getStatuses() { return statuses; }
    public void setStatuses(List<String> statuses) { this.statuses = statuses; }
    public PageRequest getPageRequest() { return pageRequest; }
    public void setPageRequest(PageRequest pageRequest) { this.pageRequest = pageRequest; }
}
