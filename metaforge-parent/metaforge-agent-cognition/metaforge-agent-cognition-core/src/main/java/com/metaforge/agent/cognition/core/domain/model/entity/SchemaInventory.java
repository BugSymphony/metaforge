package com.metaforge.agent.cognition.core.domain.model.entity;

import java.util.List;

public class SchemaInventory {
    private String bundleFqn; private List<SchemaEntry> schemas;
    public static class SchemaEntry {
        private String schemaFqn; private String name; private String description; private int instanceCount;
        public String getSchemaFqn() { return schemaFqn; } public void setSchemaFqn(String s) { this.schemaFqn = s; }
        public String getName() { return name; } public void setName(String n) { this.name = n; }
        public String getDescription() { return description; } public void setDescription(String d) { this.description = d; }
        public int getInstanceCount() { return instanceCount; } public void setInstanceCount(int i) { this.instanceCount = i; }
    }
    public String getBundleFqn() { return bundleFqn; } public void setBundleFqn(String b) { this.bundleFqn = b; }
    public List<SchemaEntry> getSchemas() { return schemas; } public void setSchemas(List<SchemaEntry> s) { this.schemas = s; }
}
