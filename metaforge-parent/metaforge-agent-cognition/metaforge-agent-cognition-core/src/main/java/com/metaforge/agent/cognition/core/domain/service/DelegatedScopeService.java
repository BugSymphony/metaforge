package com.metaforge.agent.cognition.core.domain.service;

import com.metaforge.agent.cognition.api.dto.request.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DelegatedScopeService {

    private static final Logger log = LoggerFactory.getLogger(DelegatedScopeService.class);

    public Scope computeDelegatedScope(Scope parentScope, Scope subtaskScope) {
        log.debug("计算委派 scope: parent={}, subtask={}", parentScope, subtaskScope);

        if (parentScope == null || parentScope.isEmpty()) {
            log.info("父 scope 为空，委派 scope = 子任务 scope");
            return subtaskScope != null ? subtaskScope : Scope.EMPTY;
        }

        if (subtaskScope == null || subtaskScope.isEmpty()) {
            log.info("子任务 scope 为空，委派 scope = 父 scope");
            return parentScope;
        }

        List<String> bundles = intersect(parentScope.bundles(), subtaskScope.bundles());
        List<String> packages = intersect(parentScope.packages(), subtaskScope.packages());
        List<String> domainGroups = intersect(parentScope.domainGroups(), subtaskScope.domainGroups());
        List<String> domains = intersect(parentScope.domains(), subtaskScope.domains());
        List<String> entitySchemas = intersect(parentScope.entitySchemas(), subtaskScope.entitySchemas());

        Scope delegatedScope = new Scope(bundles, packages, domainGroups, domains, entitySchemas);

        log.info("三级收窄完成: bundles {}→{}, domains {}→{}, entitySchemas {}→{}",
                parentScope.bundles().size(), delegatedScope.bundles().size(),
                parentScope.domains().size(), delegatedScope.domains().size(),
                parentScope.entitySchemas().size(), delegatedScope.entitySchemas().size());

        return delegatedScope;
    }

    public List<String> narrowBundleScope(Scope parentScope, Scope subtaskScope) {
        List<String> parentBundles = parentScope != null ? parentScope.bundles() : List.of();
        List<String> subtaskBundles = subtaskScope != null ? subtaskScope.bundles() : List.of();

        if (parentBundles.isEmpty()) return subtaskBundles;
        if (subtaskBundles.isEmpty()) return parentBundles;

        return intersect(parentBundles, subtaskBundles);
    }

    public List<String> narrowDomainScope(Scope parentScope, Scope subtaskScope) {
        List<String> parentDomains = new ArrayList<>();
        if (parentScope != null) {
            parentDomains.addAll(parentScope.domains());
            parentDomains.addAll(parentScope.domainGroups());
        }

        List<String> subtaskDomains = new ArrayList<>();
        if (subtaskScope != null) {
            subtaskDomains.addAll(subtaskScope.domains());
            subtaskDomains.addAll(subtaskScope.domainGroups());
        }

        if (parentDomains.isEmpty()) return subtaskDomains;
        if (subtaskDomains.isEmpty()) return parentDomains;

        return intersect(parentDomains, subtaskDomains);
    }

    public List<String> narrowEntitySchemaScope(Scope parentScope, Scope subtaskScope) {
        List<String> parentSchemas = parentScope != null ? parentScope.entitySchemas() : List.of();
        List<String> subtaskSchemas = subtaskScope != null ? subtaskScope.entitySchemas() : List.of();

        if (parentSchemas.isEmpty()) return subtaskSchemas;
        if (subtaskSchemas.isEmpty()) return parentSchemas;

        return intersect(parentSchemas, subtaskSchemas);
    }

    private List<String> intersect(List<String> a, List<String> b) {
        if (a == null || a.isEmpty()) return b != null ? new ArrayList<>(b) : List.of();
        if (b == null || b.isEmpty()) return new ArrayList<>(a);

        return a.stream()
                .filter(item -> b.stream().anyMatch(item::equalsIgnoreCase))
                .collect(Collectors.toList());
    }
}
