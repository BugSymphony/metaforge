package com.metaforge.metadata.api.dto.response;

public class ImportItemResult {
    private String fqn;
    private boolean success;
    private String message;

    public ImportItemResult() {}

    public String getFqn() { return fqn; }
    public void setFqn(String fqn) { this.fqn = fqn; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
