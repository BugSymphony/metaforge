package com.metaforge.agent.cognition.api.dto.response;

import java.util.List;

public class DecisionMatrix {

    private List<DecisionPoint> decisionPoints;

    public List<DecisionPoint> getDecisionPoints() { return decisionPoints; }
    public void setDecisionPoints(List<DecisionPoint> decisionPoints) { this.decisionPoints = decisionPoints; }

    public static class DecisionPoint {
        private String decisionEntityFqn;
        private String decisionName;
        private List<DecisionOption> options;
        private String recommendation;

        public String getDecisionEntityFqn() { return decisionEntityFqn; }
        public void setDecisionEntityFqn(String decisionEntityFqn) { this.decisionEntityFqn = decisionEntityFqn; }
        public String getDecisionName() { return decisionName; }
        public void setDecisionName(String decisionName) { this.decisionName = decisionName; }
        public List<DecisionOption> getOptions() { return options; }
        public void setOptions(List<DecisionOption> options) { this.options = options; }
        public String getRecommendation() { return recommendation; }
        public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

        public static class DecisionOption {
            private String targetEntityFqn;
            private String triggerCondition;
            private List<String> downstreamImpact;

            public String getTargetEntityFqn() { return targetEntityFqn; }
            public void setTargetEntityFqn(String targetEntityFqn) { this.targetEntityFqn = targetEntityFqn; }
            public String getTriggerCondition() { return triggerCondition; }
            public void setTriggerCondition(String triggerCondition) { this.triggerCondition = triggerCondition; }
            public List<String> getDownstreamImpact() { return downstreamImpact; }
            public void setDownstreamImpact(List<String> downstreamImpact) { this.downstreamImpact = downstreamImpact; }
        }
    }
}
