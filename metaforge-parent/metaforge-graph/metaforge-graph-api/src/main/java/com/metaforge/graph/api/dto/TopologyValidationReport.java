package com.metaforge.graph.api.dto;

import java.util.List;

/**
 * 拓扑校验报告 DTO。
 */
public class TopologyValidationReport {

    private int totalChecked;
    private int issuesFound;
    private List<TopologyIssue> issues;

    public TopologyValidationReport() {}

    public static TopologyValidationReport empty() {
        TopologyValidationReport report = new TopologyValidationReport();
        report.totalChecked = 0;
        report.issuesFound = 0;
        report.issues = List.of();
        return report;
    }

    public int getTotalChecked() { return totalChecked; }
    public void setTotalChecked(int totalChecked) { this.totalChecked = totalChecked; }

    public int getIssuesFound() { return issuesFound; }
    public void setIssuesFound(int issuesFound) { this.issuesFound = issuesFound; }

    public List<TopologyIssue> getIssues() { return issues; }
    public void setIssues(List<TopologyIssue> issues) { this.issues = issues; }

    public static class TopologyIssue {
        private String relationFqn;
        private IssueType issueType;
        private String description;

        public TopologyIssue() {}

        public String getRelationFqn() { return relationFqn; }
        public void setRelationFqn(String relationFqn) { this.relationFqn = relationFqn; }

        public IssueType getIssueType() { return issueType; }
        public void setIssueType(IssueType issueType) { this.issueType = issueType; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public enum IssueType {
        DANGLING_EDGE,
        INVALID_ENDPOINT,
        CARDINALITY_ERROR,
        SCHEMA_MISMATCH
    }
}
