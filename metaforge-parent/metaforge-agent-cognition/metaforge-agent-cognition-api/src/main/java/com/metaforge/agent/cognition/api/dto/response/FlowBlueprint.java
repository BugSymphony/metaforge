package com.metaforge.agent.cognition.api.dto.response;

import java.util.List;

public class FlowBlueprint {

    private String bundleFqn;
    private List<FlowStep> steps;
    private String entryStep;
    private List<String> exitSteps;
    private List<BranchPoint> branchPoints;
    private boolean empty;
    private String emptyNote;

    public String getBundleFqn() { return bundleFqn; }
    public void setBundleFqn(String bundleFqn) { this.bundleFqn = bundleFqn; }
    public List<FlowStep> getSteps() { return steps; }
    public void setSteps(List<FlowStep> steps) { this.steps = steps; }
    public String getEntryStep() { return entryStep; }
    public void setEntryStep(String entryStep) { this.entryStep = entryStep; }
    public List<String> getExitSteps() { return exitSteps; }
    public void setExitSteps(List<String> exitSteps) { this.exitSteps = exitSteps; }
    public List<BranchPoint> getBranchPoints() { return branchPoints; }
    public void setBranchPoints(List<BranchPoint> branchPoints) { this.branchPoints = branchPoints; }
    public boolean isEmpty() { return empty; }
    public void setEmpty(boolean empty) { this.empty = empty; }
    public String getEmptyNote() { return emptyNote; }
    public void setEmptyNote(String emptyNote) { this.emptyNote = emptyNote; }

    public static class FlowStep {
        private String stepFqn;
        private String name;
        private String description;
        private List<String> preconditions;
        private List<String> outputs;
        private int sequenceOrder;

        public String getStepFqn() { return stepFqn; }
        public void setStepFqn(String stepFqn) { this.stepFqn = stepFqn; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<String> getPreconditions() { return preconditions; }
        public void setPreconditions(List<String> preconditions) { this.preconditions = preconditions; }
        public List<String> getOutputs() { return outputs; }
        public void setOutputs(List<String> outputs) { this.outputs = outputs; }
        public int getSequenceOrder() { return sequenceOrder; }
        public void setSequenceOrder(int sequenceOrder) { this.sequenceOrder = sequenceOrder; }
    }

    public static class BranchPoint {
        private String decisionStepFqn;
        private List<String> alternativePaths;

        public String getDecisionStepFqn() { return decisionStepFqn; }
        public void setDecisionStepFqn(String decisionStepFqn) { this.decisionStepFqn = decisionStepFqn; }
        public List<String> getAlternativePaths() { return alternativePaths; }
        public void setAlternativePaths(List<String> alternativePaths) { this.alternativePaths = alternativePaths; }
    }
}
