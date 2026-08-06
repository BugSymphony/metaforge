package com.metaforge.agent.cognition.api.dto.response;

import java.util.List;

public class ConstraintSet {

    private List<ConstraintItem> constraints;
    private List<HardBoundary> hardBoundaries;
    private List<SoftBoundary> softBoundaries;
    private boolean empty;
    private String emptyNote;

    public List<ConstraintItem> getConstraints() { return constraints; }
    public void setConstraints(List<ConstraintItem> constraints) { this.constraints = constraints; }
    public List<HardBoundary> getHardBoundaries() { return hardBoundaries; }
    public void setHardBoundaries(List<HardBoundary> hardBoundaries) { this.hardBoundaries = hardBoundaries; }
    public List<SoftBoundary> getSoftBoundaries() { return softBoundaries; }
    public void setSoftBoundaries(List<SoftBoundary> softBoundaries) { this.softBoundaries = softBoundaries; }
    public boolean isEmpty() { return empty; }
    public void setEmpty(boolean empty) { this.empty = empty; }
    public String getEmptyNote() { return emptyNote; }
    public void setEmptyNote(String emptyNote) { this.emptyNote = emptyNote; }

    public static class ConstraintItem {
        private String constraintFqn;
        private String constraintName;
        private String constraintLevel;
        private String constraintDescription;
        private String sourceEntityFqn;
        private String sourceType;

        public String getConstraintFqn() { return constraintFqn; }
        public void setConstraintFqn(String constraintFqn) { this.constraintFqn = constraintFqn; }
        public String getConstraintName() { return constraintName; }
        public void setConstraintName(String constraintName) { this.constraintName = constraintName; }
        public String getConstraintLevel() { return constraintLevel; }
        public void setConstraintLevel(String constraintLevel) { this.constraintLevel = constraintLevel; }
        public String getConstraintDescription() { return constraintDescription; }
        public void setConstraintDescription(String constraintDescription) { this.constraintDescription = constraintDescription; }
        public String getSourceEntityFqn() { return sourceEntityFqn; }
        public void setSourceEntityFqn(String sourceEntityFqn) { this.sourceEntityFqn = sourceEntityFqn; }
        public String getSourceType() { return sourceType; }
        public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    }

    public static class HardBoundary {
        private String fieldName;
        private boolean required;
        private List<String> enumValues;
        private Object minimum;
        private Object maximum;
        private String pattern;

        public String getFieldName() { return fieldName; }
        public void setFieldName(String fieldName) { this.fieldName = fieldName; }
        public boolean isRequired() { return required; }
        public void setRequired(boolean required) { this.required = required; }
        public List<String> getEnumValues() { return enumValues; }
        public void setEnumValues(List<String> enumValues) { this.enumValues = enumValues; }
        public Object getMinimum() { return minimum; }
        public void setMinimum(Object minimum) { this.minimum = minimum; }
        public Object getMaximum() { return maximum; }
        public void setMaximum(Object maximum) { this.maximum = maximum; }
        public String getPattern() { return pattern; }
        public void setPattern(String pattern) { this.pattern = pattern; }
    }

    public static class SoftBoundary {
        private String referenceEntityFqn;
        private String referenceEntityName;
        private String referenceDescription;

        public String getReferenceEntityFqn() { return referenceEntityFqn; }
        public void setReferenceEntityFqn(String referenceEntityFqn) { this.referenceEntityFqn = referenceEntityFqn; }
        public String getReferenceEntityName() { return referenceEntityName; }
        public void setReferenceEntityName(String referenceEntityName) { this.referenceEntityName = referenceEntityName; }
        public String getReferenceDescription() { return referenceDescription; }
        public void setReferenceDescription(String referenceDescription) { this.referenceDescription = referenceDescription; }
    }
}
