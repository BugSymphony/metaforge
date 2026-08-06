package com.metaforge.agent.cognition.core.domain.model.entity;

import java.util.List;

public class FlowBlueprint {
    private String bundleFqn; private List<FlowStep> steps; private String entryStep;
    private List<String> exitSteps; private List<BranchPoint> branchPoints; private boolean empty; private String emptyNote;
    public static class FlowStep {
        private String stepFqn; private String name; private String description;
        private List<String> preconditions; private List<String> outputs; private int sequenceOrder;
        public String getStepFqn() { return stepFqn; } public void setStepFqn(String s) { this.stepFqn = s; }
        public String getName() { return name; } public void setName(String n) { this.name = n; }
        public String getDescription() { return description; } public void setDescription(String d) { this.description = d; }
        public List<String> getPreconditions() { return preconditions; } public void setPreconditions(List<String> p) { this.preconditions = p; }
        public List<String> getOutputs() { return outputs; } public void setOutputs(List<String> o) { this.outputs = o; }
        public int getSequenceOrder() { return sequenceOrder; } public void setSequenceOrder(int s) { this.sequenceOrder = s; }
    }
    public static class BranchPoint {
        private String decisionStepFqn; private List<String> alternativePaths;
        public String getDecisionStepFqn() { return decisionStepFqn; } public void setDecisionStepFqn(String d) { this.decisionStepFqn = d; }
        public List<String> getAlternativePaths() { return alternativePaths; } public void setAlternativePaths(List<String> a) { this.alternativePaths = a; }
    }
    public String getBundleFqn() { return bundleFqn; } public void setBundleFqn(String b) { this.bundleFqn = b; }
    public List<FlowStep> getSteps() { return steps; } public void setSteps(List<FlowStep> s) { this.steps = s; }
    public String getEntryStep() { return entryStep; } public void setEntryStep(String e) { this.entryStep = e; }
    public List<String> getExitSteps() { return exitSteps; } public void setExitSteps(List<String> e) { this.exitSteps = e; }
    public List<BranchPoint> getBranchPoints() { return branchPoints; } public void setBranchPoints(List<BranchPoint> b) { this.branchPoints = b; }
    public boolean isEmpty() { return empty; } public void setEmpty(boolean e) { this.empty = e; }
    public String getEmptyNote() { return emptyNote; } public void setEmptyNote(String e) { this.emptyNote = e; }
}
