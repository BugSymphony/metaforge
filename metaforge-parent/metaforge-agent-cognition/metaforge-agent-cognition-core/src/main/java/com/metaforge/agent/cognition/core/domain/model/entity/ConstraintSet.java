package com.metaforge.agent.cognition.core.domain.model.entity;

import java.util.List;

public class ConstraintSet {
    private List<ConstraintItem> constraints; private List<HardBoundary> hardBoundaries;
    private List<SoftBoundary> softBoundaries; private boolean empty; private String emptyNote;
    public static class ConstraintItem {
        private String constraintFqn; private String constraintName; private String constraintLevel;
        private String constraintDescription; private String sourceEntityFqn; private String sourceType;
        public String getConstraintFqn() { return constraintFqn; } public void setConstraintFqn(String c) { this.constraintFqn = c; }
        public String getConstraintName() { return constraintName; } public void setConstraintName(String c) { this.constraintName = c; }
        public String getConstraintLevel() { return constraintLevel; } public void setConstraintLevel(String c) { this.constraintLevel = c; }
        public String getConstraintDescription() { return constraintDescription; } public void setConstraintDescription(String c) { this.constraintDescription = c; }
        public String getSourceEntityFqn() { return sourceEntityFqn; } public void setSourceEntityFqn(String c) { this.sourceEntityFqn = c; }
        public String getSourceType() { return sourceType; } public void setSourceType(String c) { this.sourceType = c; }
    }
    public static class HardBoundary {
        private String fieldName; private boolean required; private List<String> enumValues;
        private Object minimum; private Object maximum; private String pattern;
        public String getFieldName() { return fieldName; } public void setFieldName(String f) { this.fieldName = f; }
        public boolean isRequired() { return required; } public void setRequired(boolean r) { this.required = r; }
        public List<String> getEnumValues() { return enumValues; } public void setEnumValues(List<String> e) { this.enumValues = e; }
        public Object getMinimum() { return minimum; } public void setMinimum(Object m) { this.minimum = m; }
        public Object getMaximum() { return maximum; } public void setMaximum(Object m) { this.maximum = m; }
        public String getPattern() { return pattern; } public void setPattern(String p) { this.pattern = p; }
    }
    public static class SoftBoundary {
        private String referenceEntityFqn; private String referenceEntityName; private String referenceDescription;
        public String getReferenceEntityFqn() { return referenceEntityFqn; } public void setReferenceEntityFqn(String r) { this.referenceEntityFqn = r; }
        public String getReferenceEntityName() { return referenceEntityName; } public void setReferenceEntityName(String r) { this.referenceEntityName = r; }
        public String getReferenceDescription() { return referenceDescription; } public void setReferenceDescription(String r) { this.referenceDescription = r; }
    }
    public List<ConstraintItem> getConstraints() { return constraints; } public void setConstraints(List<ConstraintItem> c) { this.constraints = c; }
    public List<HardBoundary> getHardBoundaries() { return hardBoundaries; } public void setHardBoundaries(List<HardBoundary> h) { this.hardBoundaries = h; }
    public List<SoftBoundary> getSoftBoundaries() { return softBoundaries; } public void setSoftBoundaries(List<SoftBoundary> s) { this.softBoundaries = s; }
    public boolean isEmpty() { return empty; } public void setEmpty(boolean e) { this.empty = e; }
    public String getEmptyNote() { return emptyNote; } public void setEmptyNote(String e) { this.emptyNote = e; }
}
