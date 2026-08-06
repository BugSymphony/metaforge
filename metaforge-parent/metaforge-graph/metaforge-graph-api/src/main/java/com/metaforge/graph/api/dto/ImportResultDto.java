package com.metaforge.graph.api.dto;

import java.util.List;

/**
 * 导入结果 DTO。
 */
public class ImportResultDto {

    private int totalCount;
    private int successCount;
    private int skipCount;
    private int failureCount;
    private List<ImportItemResult> items;

    public ImportResultDto() {}

    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }

    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }

    public int getSkipCount() { return skipCount; }
    public void setSkipCount(int skipCount) { this.skipCount = skipCount; }

    public int getFailureCount() { return failureCount; }
    public void setFailureCount(int failureCount) { this.failureCount = failureCount; }

    public List<ImportItemResult> getItems() { return items; }
    public void setItems(List<ImportItemResult> items) { this.items = items; }
}
