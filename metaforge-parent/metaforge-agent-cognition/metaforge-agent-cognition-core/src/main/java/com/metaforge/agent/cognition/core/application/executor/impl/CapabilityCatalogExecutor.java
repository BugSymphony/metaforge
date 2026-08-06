package com.metaforge.agent.cognition.core.application.executor.impl;

import com.metaforge.agent.cognition.api.enums.ContextMode;
import com.metaforge.agent.cognition.api.enums.PerspectiveCode;
import com.metaforge.agent.cognition.api.perspective.PerspectiveExecutionContext;
import com.metaforge.agent.cognition.api.perspective.PerspectiveExecutor;
import com.metaforge.agent.cognition.core.domain.model.entity.CapabilityCatalog;
import com.metaforge.metadata.api.dto.response.MetadataEntityDto;
import com.metaforge.metamodel.api.dto.response.EntitySchemaDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CapabilityCatalogExecutor implements PerspectiveExecutor {

    private static final Logger log = LoggerFactory.getLogger(CapabilityCatalogExecutor.class);

    private final ExecutorSupport support;

    public CapabilityCatalogExecutor(ExecutorSupport support) {
        this.support = support;
    }

    @Override
    public PerspectiveCode supportedPerspective() {
        return PerspectiveCode.CAPABILITY_CATALOG;
    }

    @Override
    public Object execute(PerspectiveExecutionContext ctx) {
        log.debug("执行能力目录视角: entityFqn={}, contextMode={}", ctx.entityFqn(), ctx.contextMode());

        CapabilityCatalog catalog = new CapabilityCatalog();
        catalog.setCapabilities(new ArrayList<>());

        List<String> schemaFqns = findSchemaFqns(ctx, "Capability");
        if (schemaFqns.isEmpty()) {
            return catalog;
        }

        List<CapabilityCatalog.CapabilityItem> items = new ArrayList<>();
        for (String schemaFqn : schemaFqns) {
            Object raw = support.metadata().listByEntitySchema(schemaFqn, 1, Integer.MAX_VALUE);
            for (MetadataEntityDto entity : support.entities(raw)) {
                CapabilityCatalog.CapabilityItem item = new CapabilityCatalog.CapabilityItem();
                item.setCapabilityFqn(entity.getFqn());
                item.setName(entity.getName() != null ? entity.getName() : entity.getFqn());
                item.setDescription(entity.getDescription());
                item.setInterfaceSpec(stringifyContent(entity, "interface_spec"));
                item.setCallMethod(stringifyContent(entity, "call_method"));
                item.setProtocols(new ArrayList<>());
                items.add(item);
            }
        }
        catalog.setCapabilities(items);
        return catalog;
    }

    private List<String> findSchemaFqns(PerspectiveExecutionContext ctx, String schemaSimpleName) {
        List<String> result = new ArrayList<>();
        if (ctx.bundleFqns() == null) {
            return result;
        }
        for (String bundleCode : ctx.bundleFqns()) {
            String prefix = support.resolveVersionedPrefix(bundleCode);
            if (prefix == null) {
                continue;
            }
            Object rawSchemas = support.metamodel().listEntitySchemasByPrefixes(List.of(prefix));
            for (EntitySchemaDto schema : support.schemas(rawSchemas)) {
                if (schema.getName() != null && schema.getName().equals(schemaSimpleName)) {
                    result.add(schema.getFqn());
                }
            }
        }
        return result;
    }

    private String stringifyContent(MetadataEntityDto entity, String key) {
        if (entity.getContent() == null) {
            return null;
        }
        Object value = entity.getContent().get(key);
        return value != null ? String.valueOf(value) : null;
    }
}
