package com.metaforge.agent.cognition.core.domain.model.entity;

import com.metaforge.agent.cognition.core.domain.model.valueobject.PerspectiveCode;

public class PerspectiveResult {

    private PerspectiveCode perspectiveCode;
    private Object data;
    private boolean truncated;
    private String truncatedReason;
    private long executionDurationMs;

    public PerspectiveCode getPerspectiveCode() { return perspectiveCode; }
    public void setPerspectiveCode(PerspectiveCode perspectiveCode) { this.perspectiveCode = perspectiveCode; }
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
    public boolean isTruncated() { return truncated; }
    public void setTruncated(boolean truncated) { this.truncated = truncated; }
    public String getTruncatedReason() { return truncatedReason; }
    public void setTruncatedReason(String truncatedReason) { this.truncatedReason = truncatedReason; }
    public long getExecutionDurationMs() { return executionDurationMs; }
    public void setExecutionDurationMs(long executionDurationMs) { this.executionDurationMs = executionDurationMs; }
}
