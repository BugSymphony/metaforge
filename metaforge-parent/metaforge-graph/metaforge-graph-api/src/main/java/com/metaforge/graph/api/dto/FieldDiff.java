package com.metaforge.graph.api.dto;

/**
 * 字段差异 DTO。
 */
public class FieldDiff {

    private String fieldPath;
    private Object oldValue;
    private Object newValue;

    public FieldDiff() {}

    public static FieldDiff added(String path, Object value) {
        FieldDiff diff = new FieldDiff();
        diff.fieldPath = path;
        diff.newValue = value;
        return diff;
    }

    public static FieldDiff deleted(String path, Object value) {
        FieldDiff diff = new FieldDiff();
        diff.fieldPath = path;
        diff.oldValue = value;
        return diff;
    }

    public static FieldDiff modified(String path, Object oldVal, Object newVal) {
        FieldDiff diff = new FieldDiff();
        diff.fieldPath = path;
        diff.oldValue = oldVal;
        diff.newValue = newVal;
        return diff;
    }

    public String getFieldPath() { return fieldPath; }
    public void setFieldPath(String fieldPath) { this.fieldPath = fieldPath; }

    public Object getOldValue() { return oldValue; }
    public void setOldValue(Object oldValue) { this.oldValue = oldValue; }

    public Object getNewValue() { return newValue; }
    public void setNewValue(Object newValue) { this.newValue = newValue; }
}
