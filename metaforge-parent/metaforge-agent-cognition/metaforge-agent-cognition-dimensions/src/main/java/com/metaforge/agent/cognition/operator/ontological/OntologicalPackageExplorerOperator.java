package com.metaforge.agent.cognition.operator.ontological;

import com.metaforge.agent.cognition.api.dto.request.Scope;
import com.metaforge.agent.cognition.api.enums.DimensionCategory;
import com.metaforge.agent.cognition.api.spi.CognitionQueryContext;
import com.metaforge.agent.cognition.api.spi.CognitionResult;
import com.metaforge.agent.cognition.operator.common.AbstractCognitionOperator;
import com.metaforge.metamodel.api.dto.response.PackageDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OntologicalPackageExplorerOperator extends AbstractCognitionOperator {

    @Override
    public String operatorId() {
        return "ontological.package-explorer";
    }

    @Override
    public DimensionCategory category() {
        return DimensionCategory.ONTOLOGICAL;
    }

    @Override
    public CognitionResult execute(CognitionQueryContext context) {
        String bundleVersionFqn = getBundleVersionFqn(context);
        if (bundleVersionFqn == null || bundleVersionFqn.isBlank()) {
            return wrapFailure("缺少 bundleVersionFqn 参数");
        }

        Object portResult = executeWithPort(() -> metamodelReadPort.listPackages(bundleVersionFqn));
        if (portResult instanceof CognitionResult cr) {
            return cr;
        }

        List<Map<String, Object>> packages = extractList(portResult);

        Scope scope = context.scope();
        boolean scopeHasPackages = scope != null && scope.packages() != null && !scope.packages().isEmpty();
        ScopeFilterResult filtered = applyScope(packages, scope);

        List<Map<String, Object>> lazyNodes = new ArrayList<>();
        for (Map<String, Object> pkg : filtered.inScopeItems()) {
            String pkgFqn = (String) pkg.get("fqn");
            boolean hasChildren = checkHasCompositionChildren(pkgFqn);
            String nextCall = hasChildren ? "ontological.package-explorer" : "ontological.entity-schema-inventory";
            lazyNodes.add(buildLazyNode(pkg, hasChildren, nextCall));
        }

        Map<String, Object> resultData = new LinkedHashMap<>();
        resultData.put("packages", lazyNodes);
        if (scopeHasPackages) {
            List<String> discovered = filtered.inScopeItems().stream()
                    .map(p -> (String) p.get("fqn"))
                    .filter(f -> f != null)
                    .toList();
            resultData.put("updated_scope", Map.of("packages", discovered, "skipped", filtered.skippedFqns()));
        }

        return CognitionResult.success(operatorId(), category(), lazyNodes);
    }

    private boolean checkHasCompositionChildren(String entityFqn) {
        if (entityFqn == null) return false;
        Object result = executeWithPort(() -> graphReadPort.getOutboundRelations(entityFqn, "COMPOSITION", null));
        if (result instanceof CognitionResult) return false;
        if (result instanceof List<?> list) return !list.isEmpty();
        return false;
    }

    private String getBundleVersionFqn(CognitionQueryContext context) {
        Map<String, Object> params = context.templateParams();
        if (params != null) {
            if (params.containsKey("bundleVersionFqn")) {
                return (String) params.get("bundleVersionFqn");
            }
            if (params.containsKey("parent_fqn")) {
                return (String) params.get("parent_fqn");
            }
        }
        if (context.entityFqn() != null && !context.entityFqn().isBlank()) {
            return context.entityFqn();
        }
        List<String> bundleFqns = context.bundleFqns();
        if (bundleFqns != null && !bundleFqns.isEmpty()) {
            return bundleFqns.get(0);
        }
        return null;
    }

    private List<Map<String, Object>> extractList(Object portResult) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (portResult instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof PackageDto dto) {
                    result.add(toEntityMap(dto));
                } else if (item instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> m = (Map<String, Object>) item;
                    result.add(m);
                }
            }
        }
        return result;
    }
}
