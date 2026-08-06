package com.metaforge.agent.cognition.core.domain.model.entity;

import java.util.List;

public class DecisionMatrix {
    private List<DecisionPoint> decisionPoints;
    public static class DecisionPoint {
        private String decisionEntityFqn; private String decisionName; private List<DecisionOption> options; private String recommendation;
        public String getDecisionEntityFqn() { return decisionEntityFqn; } public void setDecisionEntityFqn(String d) { this.decisionEntityFqn = d; }
        public String getDecisionName() { return decisionName; } public void setDecisionName(String d) { this.decisionName = d; }
        public List<DecisionOption> getOptions() { return options; } public void setOptions(List<DecisionOption> o) { this.options = o; }
        public String getRecommendation() { return recommendation; } public void setRecommendation(String r) { this.recommendation = r; }
        public static class DecisionOption {
            private String targetEntityFqn; private String triggerCondition; private List<String> downstreamImpact;
            public String getTargetEntityFqn() { return targetEntityFqn; } public void setTargetEntityFqn(String t) { this.targetEntityFqn = t; }
            public String getTriggerCondition() { return triggerCondition; } public void setTriggerCondition(String t) { this.triggerCondition = t; }
            public List<String> getDownstreamImpact() { return downstreamImpact; } public void setDownstreamImpact(List<String> d) { this.downstreamImpact = d; }
        }
    }
    public List<DecisionPoint> getDecisionPoints() { return decisionPoints; } public void setDecisionPoints(List<DecisionPoint> d) { this.decisionPoints = d; }
}
