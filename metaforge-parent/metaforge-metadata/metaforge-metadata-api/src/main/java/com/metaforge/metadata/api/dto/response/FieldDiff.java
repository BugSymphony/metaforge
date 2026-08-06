package com.metaforge.metadata.api.dto.response;

import com.metaforge.metadata.api.enums.DiffType;

public class FieldDiff {
    private String path;
    private DiffType diffType;
    private Object oldValue;
    private Object newValue;

    public FieldDiff() {}

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public DiffType getDiffType() { return diffType; }
    public void setDiffType(DiffType diffType) { this.diffType = diffType; }
    public Object getOldValue() { return oldValue; }
    public void setOldValue(Object oldValue) { this.oldValue = oldValue; }
    public Object getNewValue() { return newValue; }
    public void setNewValue(Object newValue) { this.newValue = newValue; }
}
