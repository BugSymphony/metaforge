package com.metaforge.agent.cognition.core.application.executor.impl;

import com.metaforge.agent.cognition.api.enums.PerspectiveCode;
import com.metaforge.agent.cognition.api.perspective.PerspectiveExecutionContext;
import com.metaforge.agent.cognition.api.perspective.PerspectiveExecutor;
import com.metaforge.agent.cognition.core.domain.model.entity.DomainNavigation;
import com.metaforge.metamodel.api.dto.response.BundleDto;
import com.metaforge.metamodel.api.dto.response.EntitySchemaDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DomainNavigationExecutor implements PerspectiveExecutor {

    private static final Logger log = LoggerFactory.getLogger(DomainNavigationExecutor.class);

    private final ExecutorSupport support;

    public DomainNavigationExecutor(ExecutorSupport support) {
        this.support = support;
    }

    @Override
    public PerspectiveCode supportedPerspective() {
        return PerspectiveCode.DOMAIN_NAVIGATION;
    }

    @Override
    public Object execute(PerspectiveExecutionContext ctx) {
        log.debug("执行主题域导航视角: anchorFqn={}", ctx.entityFqn());

        DomainNavigation nav = new DomainNavigation();
        nav.setAnchorFqn(ctx.entityFqn());
        nav.setCurrentLevel("L1");
        nav.setChildren(new ArrayList<>());
        nav.setHasMore(false);

        if (ctx.entityFqn() == null) {
            return nav;
        }

        String bundleCode = extractBundleCode(ctx.entityFqn());
        if (bundleCode == null) {
            return nav;
        }

        String prefix = support.resolveVersionedPrefix(bundleCode);
        if (prefix == null) {
            return nav;
        }

        Object rawSchemas = support.metamodel().listEntitySchemasByPrefixes(List.of(prefix));
        List<EntitySchemaDto> schemas = support.schemas(rawSchemas);
        if (schemas == null || schemas.isEmpty()) {
            return nav;
        }

        List<DomainNavigation.NavNode> children = new ArrayList<>();
        for (EntitySchemaDto schema : schemas) {
            if (!schema.getFqn().startsWith(prefix + ".")) {
                continue;
            }
            DomainNavigation.NavNode node = new DomainNavigation.NavNode();
            node.setFqn(schema.getFqn());
            node.setName(schema.getName() != null ? schema.getName() : schema.getFqn());
            node.setDescription(schema.getDescription());
            node.setChildCount(0);
            node.setHasMoreChildren(false);
            children.add(node);
        }
        nav.setChildren(children);
        nav.setCurrentLevel("L2");
        nav.setHasMore(false);
        return nav;
    }

    private String extractBundleCode(String fqn) {
        if (fqn == null) return null;
        int colon = fqn.indexOf(':');
        return colon > 0 ? fqn.substring(0, colon) : fqn;
    }
}
