package com.metaforge.agent.cognition.api.dto.request;

import java.util.Collections;
import java.util.List;

public record Scope(
        List<String> bundles,
        List<String> packages,
        List<String> domainGroups,
        List<String> domains,
        List<String> entitySchemas
) {

    public static final Scope EMPTY = new Scope(
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()
    );

    public boolean isEmpty() {
        return (bundles == null || bundles.isEmpty())
                && (packages == null || packages.isEmpty())
                && (domainGroups == null || domainGroups.isEmpty())
                && (domains == null || domains.isEmpty())
                && (entitySchemas == null || entitySchemas.isEmpty());
    }

    public Scope {
        bundles = bundles != null ? bundles : Collections.emptyList();
        packages = packages != null ? packages : Collections.emptyList();
        domainGroups = domainGroups != null ? domainGroups : Collections.emptyList();
        domains = domains != null ? domains : Collections.emptyList();
        entitySchemas = entitySchemas != null ? entitySchemas : Collections.emptyList();
    }
}
