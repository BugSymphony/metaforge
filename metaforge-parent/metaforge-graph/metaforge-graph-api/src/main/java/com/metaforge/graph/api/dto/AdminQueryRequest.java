package com.metaforge.graph.api.dto;

import com.metaforge.common.dto.PageRequest;
import java.util.List;

/**
 * 管理端全状态聚合查询请求。
 */
public class AdminQueryRequest {

    private List<String> statuses;
    private String fqnPrefix;
    private String relationSchemaFqn;
    private PageRequest pageRequest;

    public AdminQueryRequest() {}

    public List<String> getStatuses() { return statuses; }
    public void setStatuses(List<String> statuses) { this.statuses = statuses; }

    public String getFqnPrefix() { return fqnPrefix; }
    public void setFqnPrefix(String fqnPrefix) { this.fqnPrefix = fqnPrefix; }

    public String getRelationSchemaFqn() { return relationSchemaFqn; }
    public void setRelationSchemaFqn(String relationSchemaFqn) { this.relationSchemaFqn = relationSchemaFqn; }

    public PageRequest getPageRequest() { return pageRequest; }
    public void setPageRequest(PageRequest pageRequest) { this.pageRequest = pageRequest; }
}
