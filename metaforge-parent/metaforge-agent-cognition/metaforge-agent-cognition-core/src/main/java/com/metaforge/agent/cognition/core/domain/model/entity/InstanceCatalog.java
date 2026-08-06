package com.metaforge.agent.cognition.core.domain.model.entity;

import java.util.List;

public class InstanceCatalog {
    private String bundleFqn; private List<String> entityTypes; private List<CatalogEntity> entities; private int totalCount;
    public static class CatalogEntity {
        private String fqn; private String name; private String entitySchemaFqn; private int relationCount; private List<CatalogRelation> relations;
        public String getFqn() { return fqn; } public void setFqn(String f) { this.fqn = f; }
        public String getName() { return name; } public void setName(String n) { this.name = n; }
        public String getEntitySchemaFqn() { return entitySchemaFqn; } public void setEntitySchemaFqn(String e) { this.entitySchemaFqn = e; }
        public int getRelationCount() { return relationCount; } public void setRelationCount(int r) { this.relationCount = r; }
        public List<CatalogRelation> getRelations() { return relations; } public void setRelations(List<CatalogRelation> r) { this.relations = r; }
    }
    public static class CatalogRelation {
        private String relationFqn; private String associationType; private String targetEntityFqn;
        public String getRelationFqn() { return relationFqn; } public void setRelationFqn(String r) { this.relationFqn = r; }
        public String getAssociationType() { return associationType; } public void setAssociationType(String a) { this.associationType = a; }
        public String getTargetEntityFqn() { return targetEntityFqn; } public void setTargetEntityFqn(String t) { this.targetEntityFqn = t; }
    }
    public String getBundleFqn() { return bundleFqn; } public void setBundleFqn(String b) { this.bundleFqn = b; }
    public List<String> getEntityTypes() { return entityTypes; } public void setEntityTypes(List<String> e) { this.entityTypes = e; }
    public List<CatalogEntity> getEntities() { return entities; } public void setEntities(List<CatalogEntity> e) { this.entities = e; }
    public int getTotalCount() { return totalCount; } public void setTotalCount(int t) { this.totalCount = t; }
}
