package com.metaforge.agent.cognition.operator.ontological;

import com.metaforge.agent.cognition.api.dto.request.Scope;
import com.metaforge.agent.cognition.api.enums.DimensionCategory;
import com.metaforge.agent.cognition.api.spi.CognitionQueryContext;
import com.metaforge.agent.cognition.api.spi.CognitionResult;
import com.metaforge.agent.cognition.operator.common.AbstractCognitionOperator;
import com.metaforge.common.dto.PageRequest;
import com.metaforge.common.dto.PageResult;
import com.metaforge.metamodel.api.dto.response.BundleDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OntologicalBundleDiscoveryOperator extends AbstractCognitionOperator {

    @Override
    public String operatorId() {
        return "ontological.bundle-discovery";
    }

    @Override
    public DimensionCategory category() {
        return DimensionCategory.ONTOLOGICAL;
    }

    @Override
    public CognitionResult execute(CognitionQueryContext context) {
        int page = resolvePage(context);
        int size = context.pageSize() > 0 ? context.pageSize() : 20;

        Object portResult = executeWithPort(() -> metamodelReadPort.listBundles(new PageRequest(page, size)));
        if (portResult instanceof CognitionResult cr) {
            return cr;
        }

        List<Map<String, Object>> bundles = extractContent(portResult);

        Scope scope = context.scope();
        ScopeFilterResult filtered = applyScope(bundles, scope);

        List<String> discoveredBundles = new ArrayList<>();
        List<Map<String, Object>> lazyNodes = new ArrayList<>();
        for (Map<String, Object> bundle : filtered.inScopeItems()) {
            String fqn = (String) bundle.get("fqn");
            if (fqn != null) {
                discoveredBundles.add(fqn);
            }
            Map<String, Object> lazyNode = buildLazyNode(bundle, true, "ontological.package-explorer");
            lazyNodes.add(lazyNode);
        }

        Map<String, Object> resultData = new LinkedHashMap<>();
        resultData.put("bundles", lazyNodes);
        resultData.put("updated_scope", Map.of(
                "bundles", discoveredBundles,
                "skipped", filtered.skippedFqns()
        ));

        return CognitionResult.success(operatorId(), category(), lazyNodes);
    }

    private int resolvePage(CognitionQueryContext context) {
        if (context.cursor() != null && context.cursor() > 0) {
            return context.cursor();
        }
        return 1;
    }

    private List<Map<String, Object>> extractContent(Object portResult) {
        if (portResult instanceof PageResult<?> pr) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : pr.getContent()) {
                if (item instanceof BundleDto dto) {
                    result.add(toEntityMap(dto));
                } else if (item instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> m = (Map<String, Object>) item;
                    result.add(m);
                }
            }
            return result;
        }
        return Collections.emptyList();
    }
}
