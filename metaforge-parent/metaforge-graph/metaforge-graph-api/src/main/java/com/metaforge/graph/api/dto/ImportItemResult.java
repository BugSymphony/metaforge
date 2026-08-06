package com.metaforge.graph.api.dto;

/**
 * 单条导入结果 DTO。
 */
public class ImportItemResult {

    private String fqn;
    private boolean success;
    private String errorMessage;

    public ImportItemResult() {}

    public static ImportItemResult success(String fqn) {
        ImportItemResult r = new ImportItemResult();
        r.fqn = fqn;
        r.success = true;
        return r;
    }

    public static ImportItemResult skip(String fqn) {
        ImportItemResult r = new ImportItemResult();
        r.fqn = fqn;
        r.success = true;
        return r;
    }

    public static ImportItemResult failure(String fqn, String error) {
        ImportItemResult r = new ImportItemResult();
        r.fqn = fqn;
        r.success = false;
        r.errorMessage = error;
        return r;
    }

    public String getFqn() { return fqn; }
    public void setFqn(String fqn) { this.fqn = fqn; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
