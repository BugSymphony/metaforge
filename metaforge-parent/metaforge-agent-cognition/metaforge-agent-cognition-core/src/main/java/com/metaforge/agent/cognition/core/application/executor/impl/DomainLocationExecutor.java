package com.metaforge.agent.cognition.core.application.executor.impl;

import com.metaforge.agent.cognition.api.enums.PerspectiveCode;
import com.metaforge.agent.cognition.api.perspective.PerspectiveExecutionContext;
import com.metaforge.agent.cognition.api.perspective.PerspectiveExecutor;
import com.metaforge.agent.cognition.core.domain.model.entity.DomainLocation;
import com.metaforge.metadata.api.dto.response.MetadataEntityDto;
import com.metaforge.metamodel.api.dto.response.BundleDto;
import com.metaforge.metamodel.api.dto.response.EntitySchemaDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DomainLocationExecutor implements PerspectiveExecutor {

    private static final Logger log = LoggerFactory.getLogger(DomainLocationExecutor.class);

    private final ExecutorSupport support;

    public DomainLocationExecutor(ExecutorSupport support) {
        this.support = support;
    }

    @Override
    public PerspectiveCode supportedPerspective() {
        return PerspectiveCode.DOMAIN_LOCATION;
    }

    @Override
    public Object execute(PerspectiveExecutionContext ctx) {
        log.debug("执行领域定位视角: entityFqn={}", ctx.entityFqn());

        DomainLocation location = new DomainLocation();
        location.setEntityFqn(ctx.entityFqn());
        location.setPath(new ArrayList<>());
        location.setComplete(false);

        if (ctx.entityFqn() == null) {
            location.setNote("未指定实体 FQN，返回空定位路径");
            return location;
        }

        List<DomainLocation.LocationNode> path = new ArrayList<>();
        int depth = 0;

        Object raw = support.metadata().getByFqn(ctx.entityFqn());
        if (raw instanceof MetadataEntityDto entity) {
            path.add(buildNode(entity.getFqn(), entity.getName(), entity.getDescription(),
                    entity.getEntitySchemaFqn(), depth++));
            String schemaFqn = entity.getEntitySchemaFqn();
            Object rawSchema = support.metamodel().getEntitySchema(schemaFqn);
            if (rawSchema instanceof EntitySchemaDto schema) {
                path.add(buildNode(schema.getFqn(), schema.getName(), schema.getDescription(),
                        schema.getFqn(), depth++));
                addAncestors(schema, path, depth);
            }
            location.setComplete(true);
        } else {
            Object rawSchema = support.metamodel().getEntitySchema(ctx.entityFqn());
            if (rawSchema instanceof EntitySchemaDto schema) {
                path.add(buildNode(schema.getFqn(), schema.getName(), schema.getDescription(),
                        schema.getFqn(), depth++));
                addAncestors(schema, path, depth);
                location.setComplete(true);
            } else {
                location.setNote("未找到实体或 Schema，返回空定位路径");
            }
        }

        location.setPath(path);
        return location;
    }

    private void addAncestors(EntitySchemaDto schema, List<DomainLocation.LocationNode> path, int startDepth) {
        int depth = startDepth;
        Object rawBundle = support.metamodel().getBundle(extractBundleCode(schema.getBundleVersionFqn()));
        if (rawBundle instanceof BundleDto bundle) {
            path.add(buildNode(bundle.getFqn(), bundle.getName(), bundle.getDescription(),
                    null, depth));
        }
    }

    private DomainLocation.LocationNode buildNode(String fqn, String name, String description,
                                                  String schemaFqn, int depth) {
        DomainLocation.LocationNode node = new DomainLocation.LocationNode();
        node.setFqn(fqn);
        node.setName(name != null ? name : extractName(fqn));
        node.setDescription(description);
        node.setEntitySchemaFqn(schemaFqn);
        node.setDepth(depth);
        return node;
    }

    private String extractBundleCode(String versionedFqn) {
        if (versionedFqn == null) return null;
        int colon = versionedFqn.indexOf(':');
        return colon > 0 ? versionedFqn.substring(0, colon) : versionedFqn;
    }

    private String extractName(String fqn) {
        if (fqn == null) return "";
        String[] parts = fqn.split("\\.");
        return parts.length > 0 ? parts[parts.length - 1] : fqn;
    }
}
