package com.metaforge.agent.cognition.core.application.executor.impl;

import com.metaforge.agent.cognition.api.enums.PerspectiveCode;
import com.metaforge.agent.cognition.api.perspective.PerspectiveExecutionContext;
import com.metaforge.agent.cognition.api.perspective.PerspectiveExecutor;
import com.metaforge.agent.cognition.core.domain.model.entity.SchemaInventory;
import com.metaforge.metadata.api.dto.response.MetadataEntityDto;
import com.metaforge.metamodel.api.dto.response.EntitySchemaDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SchemaInventoryExecutor implements PerspectiveExecutor {

    private static final Logger log = LoggerFactory.getLogger(SchemaInventoryExecutor.class);

    private final ExecutorSupport support;

    public SchemaInventoryExecutor(ExecutorSupport support) {
        this.support = support;
    }

    @Override
    public PerspectiveCode supportedPerspective() {
        return PerspectiveCode.SCHEMA_INVENTORY;
    }

    @Override
    public Object execute(PerspectiveExecutionContext ctx) {
        log.debug("执行 Schema 库存视角: bundleFqns={}", ctx.bundleFqns());

        SchemaInventory inventory = new SchemaInventory();
        inventory.setBundleFqn(ctx.bundleFqns() != null && !ctx.bundleFqns().isEmpty()
                ? ctx.bundleFqns().get(0) : "");
        inventory.setSchemas(new ArrayList<>());

        if (ctx.bundleFqns() == null || ctx.bundleFqns().isEmpty()) {
            return inventory;
        }

        List<SchemaInventory.SchemaEntry> entries = new ArrayList<>();
        for (String bundleCode : ctx.bundleFqns()) {
            String prefix = support.resolveVersionedPrefix(bundleCode);
            if (prefix == null) {
                continue;
            }
            Object raw = support.metamodel().listEntitySchemasByPrefixes(List.of(prefix));
            List<EntitySchemaDto> schemas = support.schemas(raw);
            for (EntitySchemaDto schema : schemas) {
                SchemaInventory.SchemaEntry entry = new SchemaInventory.SchemaEntry();
                entry.setSchemaFqn(schema.getFqn());
                entry.setName(schema.getName() != null ? schema.getName() : schema.getFqn());
                entry.setDescription(schema.getDescription());
                entry.setInstanceCount(countInstances(schema.getFqn()));
                entries.add(entry);
            }
        }
        inventory.setSchemas(entries);
        return inventory;
    }

    private int countInstances(String schemaFqn) {
        Object raw = support.metadata().listByEntitySchema(schemaFqn, 1, Integer.MAX_VALUE);
        List<MetadataEntityDto> entities = support.entities(raw);
        return entities != null ? entities.size() : 0;
    }
}
