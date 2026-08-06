package com.metaforge.metadata.api.dto.response;

import java.util.List;

public class ImportResultDto {
    private int totalCount;
    private int successCount;
    private int skipCount;
    private int errorCount;
    private List<ImportItemResult> items;

    public ImportResultDto() {}

    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }
    public int getSkipCount() { return skipCount; }
    public void setSkipCount(int skipCount) { this.skipCount = skipCount; }
    public int getErrorCount() { return errorCount; }
    public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
    public List<ImportItemResult> getItems() { return items; }
    public void setItems(List<ImportItemResult> items) { this.items = items; }
}
