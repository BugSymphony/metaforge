package com.metaforge.graph.api.dto;

import java.util.List;

/**
 * 版本差异结果 DTO。
 */
public class VersionDiffDto {

    private String fqn;
    private Integer versionA;
    private Integer versionB;
    private List<FieldDiff> addedFields;
    private List<FieldDiff> modifiedFields;
    private List<FieldDiff> deletedFields;

    public VersionDiffDto() {}

    public String getFqn() { return fqn; }
    public void setFqn(String fqn) { this.fqn = fqn; }

    public Integer getVersionA() { return versionA; }
    public void setVersionA(Integer versionA) { this.versionA = versionA; }

    public Integer getVersionB() { return versionB; }
    public void setVersionB(Integer versionB) { this.versionB = versionB; }

    public List<FieldDiff> getAddedFields() { return addedFields; }
    public void setAddedFields(List<FieldDiff> addedFields) { this.addedFields = addedFields; }

    public List<FieldDiff> getModifiedFields() { return modifiedFields; }
    public void setModifiedFields(List<FieldDiff> modifiedFields) { this.modifiedFields = modifiedFields; }

    public List<FieldDiff> getDeletedFields() { return deletedFields; }
    public void setDeletedFields(List<FieldDiff> deletedFields) { this.deletedFields = deletedFields; }
}
