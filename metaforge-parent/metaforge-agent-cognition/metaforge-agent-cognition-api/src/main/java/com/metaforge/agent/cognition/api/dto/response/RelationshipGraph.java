package com.metaforge.agent.cognition.api.dto.response;

import java.util.List;
import java.util.Map;

public class RelationshipGraph {

    private String centerFqn;
    private int degrees;
    private Map<String, RelationGroup> groups;
    private boolean empty;
    private String emptyNote;

    public String getCenterFqn() { return centerFqn; }
    public void setCenterFqn(String centerFqn) { this.centerFqn = centerFqn; }
    public int getDegrees() { return degrees; }
    public void setDegrees(int degrees) { this.degrees = degrees; }
    public Map<String, RelationGroup> getGroups() { return groups; }
    public void setGroups(Map<String, RelationGroup> groups) { this.groups = groups; }
    public boolean isEmpty() { return empty; }
    public void setEmpty(boolean empty) { this.empty = empty; }
    public String getEmptyNote() { return emptyNote; }
    public void setEmptyNote(String emptyNote) { this.emptyNote = emptyNote; }

    public static class RelationGroup {
        private String associationType;
        private List<RelationDetail> relations;

        public String getAssociationType() { return associationType; }
        public void setAssociationType(String associationType) { this.associationType = associationType; }
        public List<RelationDetail> getRelations() { return relations; }
        public void setRelations(List<RelationDetail> relations) { this.relations = relations; }

        public static class RelationDetail {
            private String relationFqn;
            private String sourceEntityFqn;
            private String targetEntityFqn;
            private String semanticDescription;

            public String getRelationFqn() { return relationFqn; }
            public void setRelationFqn(String relationFqn) { this.relationFqn = relationFqn; }
            public String getSourceEntityFqn() { return sourceEntityFqn; }
            public void setSourceEntityFqn(String sourceEntityFqn) { this.sourceEntityFqn = sourceEntityFqn; }
            public String getTargetEntityFqn() { return targetEntityFqn; }
            public void setTargetEntityFqn(String targetEntityFqn) { this.targetEntityFqn = targetEntityFqn; }
            public String getSemanticDescription() { return semanticDescription; }
            public void setSemanticDescription(String semanticDescription) { this.semanticDescription = semanticDescription; }
        }
    }
}
