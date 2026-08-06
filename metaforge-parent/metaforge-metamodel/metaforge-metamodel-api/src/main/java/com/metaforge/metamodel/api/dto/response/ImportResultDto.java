package com.metaforge.metamodel.api.dto.response;

import java.util.List;

public class ImportResultDto {

    private boolean success;
    private int importedCount;
    private int skippedCount;
    private List<String> errors;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public int getImportedCount() { return importedCount; }
    public void setImportedCount(int importedCount) { this.importedCount = importedCount; }
    public int getSkippedCount() { return skippedCount; }
    public void setSkippedCount(int skippedCount) { this.skippedCount = skippedCount; }
    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public static ImportResultDto success(int imported, int skipped) {
        ImportResultDto r = new ImportResultDto();
        r.success = true;
        r.importedCount = imported;
        r.skippedCount = skipped;
        r.errors = List.of();
        return r;
    }

    public static ImportResultDto failure(List<String> errors) {
        ImportResultDto r = new ImportResultDto();
        r.success = false;
        r.importedCount = 0;
        r.skippedCount = 0;
        r.errors = errors;
        return r;
    }
}
