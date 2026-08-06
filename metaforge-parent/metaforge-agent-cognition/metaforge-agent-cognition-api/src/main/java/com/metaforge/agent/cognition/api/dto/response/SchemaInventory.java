package com.metaforge.agent.cognition.api.dto.response;

import java.util.List;

public class SchemaInventory {

    private String bundleFqn;
    private List<SchemaEntry> schemas;

    public String getBundleFqn() { return bundleFqn; }
    public void setBundleFqn(String bundleFqn) { this.bundleFqn = bundleFqn; }
    public List<SchemaEntry> getSchemas() { return schemas; }
    public void setSchemas(List<SchemaEntry> schemas) { this.schemas = schemas; }

    public static class SchemaEntry {
        private String schemaFqn;
        private String name;
        private String description;
        private int instanceCount;

        public String getSchemaFqn() { return schemaFqn; }
        public void setSchemaFqn(String schemaFqn) { this.schemaFqn = schemaFqn; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public int getInstanceCount() { return instanceCount; }
        public void setInstanceCount(int instanceCount) { this.instanceCount = instanceCount; }
    }
}
