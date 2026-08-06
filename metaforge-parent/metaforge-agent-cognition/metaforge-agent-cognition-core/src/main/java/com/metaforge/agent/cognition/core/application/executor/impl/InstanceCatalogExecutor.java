package com.metaforge.agent.cognition.core.application.executor.impl;

import com.metaforge.agent.cognition.api.enums.PerspectiveCode;
import com.metaforge.agent.cognition.api.perspective.PerspectiveExecutionContext;
import com.metaforge.agent.cognition.api.perspective.PerspectiveExecutor;
import com.metaforge.agent.cognition.core.domain.model.entity.InstanceCatalog;
import com.metaforge.metadata.api.dto.response.MetadataEntityDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class InstanceCatalogExecutor implements PerspectiveExecutor {

    private static final Logger log = LoggerFactory.getLogger(InstanceCatalogExecutor.class);

    private final ExecutorSupport support;

    public InstanceCatalogExecutor(ExecutorSupport support) {
        this.support = support;
    }

    @Override
    public PerspectiveCode supportedPerspective() {
        return PerspectiveCode.INSTANCE_CATALOG;
    }

    @Override
    public Object execute(PerspectiveExecutionContext ctx) {
        log.debug("执行实例目录视角: bundleFqns={}, contextMode={}", ctx.bundleFqns(), ctx.contextMode());

        InstanceCatalog catalog = new InstanceCatalog();
        catalog.setBundleFqn(ctx.bundleFqns() != null && !ctx.bundleFqns().isEmpty()
                ? ctx.bundleFqns().get(0) : "");
        catalog.setEntityTypes(ctx.entityTypes());
        catalog.setEntities(new ArrayList<>());
        catalog.setTotalCount(0);

        if (ctx.bundleFqns() == null || ctx.bundleFqns().isEmpty()) {
            return catalog;
        }

        List<InstanceCatalog.CatalogEntity> entities = new ArrayList<>();
        int total = 0;
        for (String bundleCode : ctx.bundleFqns()) {
            String prefix = support.resolveVersionedPrefix(bundleCode);
            if (prefix == null) {
                continue;
            }
            Object rawSchemas = support.metamodel().listEntitySchemasByPrefixes(List.of(prefix));
            List<com.metaforge.metamodel.api.dto.response.EntitySchemaDto> schemas = support.schemas(rawSchemas);
            if (schemas == null || schemas.isEmpty()) {
                continue;
            }
            for (com.metaforge.metamodel.api.dto.response.EntitySchemaDto schema : schemas) {
                Object raw = support.metadata().listByEntitySchema(schema.getFqn(), 1, Integer.MAX_VALUE);
                List<MetadataEntityDto> metadataEntities = support.entities(raw);
                if (metadataEntities == null) {
                    continue;
                }
                total += metadataEntities.size();
                for (MetadataEntityDto entity : metadataEntities) {
                    if (ctx.entityTypes() != null && !ctx.entityTypes().isEmpty()
                            && !ctx.entityTypes().contains(entity.getEntitySchemaFqn())) {
                        continue;
                    }
                    InstanceCatalog.CatalogEntity ce = new InstanceCatalog.CatalogEntity();
                    ce.setFqn(entity.getFqn());
                    ce.setName(entity.getName() != null ? entity.getName() : extractName(entity.getFqn()));
                    ce.setEntitySchemaFqn(entity.getEntitySchemaFqn());
                    ce.setRelations(buildRelations(entity.getFqn()));
                    entities.add(ce);
                }
            }
        }
        catalog.setEntities(entities);
        catalog.setTotalCount(total);
        return catalog;
    }

    private List<InstanceCatalog.CatalogRelation> buildRelations(String entityFqn) {
        List<InstanceCatalog.CatalogRelation> relations = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();

        for (com.metaforge.computeengine.api.dto.common.RelationSummary relation
                : support.neighborRelations(entityFqn)) {
            if (!seen.add(relation.fqn())) {
                continue;
            }
            InstanceCatalog.CatalogRelation cr = new InstanceCatalog.CatalogRelation();
            cr.setRelationFqn(relation.fqn());
            cr.setAssociationType(relation.associationType() != null
                    ? relation.associationType().name() : "RELATION");
            boolean isOutbound = entityFqn.equals(relation.sourceEntityFqn());
            cr.setTargetEntityFqn(isOutbound ? relation.targetEntityFqn() : relation.sourceEntityFqn());
            relations.add(cr);
        }
        return relations;
    }

    private String extractName(String fqn) {
        if (fqn == null) return "";
        String[] parts = fqn.split("\\.");
        return parts.length > 0 ? parts[parts.length - 1] : fqn;
    }
}
