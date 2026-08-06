package com.metaforge.agent.cognition.api.dto.response;

import java.util.List;

public class InstanceCatalog {

    private String bundleFqn;
    private List<String> entityTypes;
    private List<CatalogEntity> entities;
    private int totalCount;

    public String getBundleFqn() { return bundleFqn; }
    public void setBundleFqn(String bundleFqn) { this.bundleFqn = bundleFqn; }
    public List<String> getEntityTypes() { return entityTypes; }
    public void setEntityTypes(List<String> entityTypes) { this.entityTypes = entityTypes; }
    public List<CatalogEntity> getEntities() { return entities; }
    public void setEntities(List<CatalogEntity> entities) { this.entities = entities; }
    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }

    public static class CatalogEntity {
        private String fqn;
        private String name;
        private String entitySchemaFqn;
        private int relationCount;
        private List<CatalogRelation> relations;

        public String getFqn() { return fqn; }
        public void setFqn(String fqn) { this.fqn = fqn; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEntitySchemaFqn() { return entitySchemaFqn; }
        public void setEntitySchemaFqn(String entitySchemaFqn) { this.entitySchemaFqn = entitySchemaFqn; }
        public int getRelationCount() { return relationCount; }
        public void setRelationCount(int relationCount) { this.relationCount = relationCount; }
        public List<CatalogRelation> getRelations() { return relations; }
        public void setRelations(List<CatalogRelation> relations) { this.relations = relations; }
    }

    public static class CatalogRelation {
        private String relationFqn;
        private String associationType;
        private String targetEntityFqn;

        public String getRelationFqn() { return relationFqn; }
        public void setRelationFqn(String relationFqn) { this.relationFqn = relationFqn; }
        public String getAssociationType() { return associationType; }
        public void setAssociationType(String associationType) { this.associationType = associationType; }
        public String getTargetEntityFqn() { return targetEntityFqn; }
        public void setTargetEntityFqn(String targetEntityFqn) { this.targetEntityFqn = targetEntityFqn; }
    }
}
