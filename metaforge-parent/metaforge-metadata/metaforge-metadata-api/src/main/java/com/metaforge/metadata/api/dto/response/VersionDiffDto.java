package com.metaforge.metadata.api.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public class VersionDiffDto {
    private String fqn;
    private int versionA;
    private int versionB;
    private List<FieldDiff> diffs;
    private LocalDateTime diffTime;

    public VersionDiffDto() {}

    public String getFqn() { return fqn; }
    public void setFqn(String fqn) { this.fqn = fqn; }
    public int getVersionA() { return versionA; }
    public void setVersionA(int versionA) { this.versionA = versionA; }
    public int getVersionB() { return versionB; }
    public void setVersionB(int versionB) { this.versionB = versionB; }
    public List<FieldDiff> getDiffs() { return diffs; }
    public void setDiffs(List<FieldDiff> diffs) { this.diffs = diffs; }
    public LocalDateTime getDiffTime() { return diffTime; }
    public void setDiffTime(LocalDateTime diffTime) { this.diffTime = diffTime; }
}
