package com.metaforge.agent.cognition.operator.relational;

import com.metaforge.agent.cognition.api.enums.DimensionCategory;
import com.metaforge.agent.cognition.api.spi.CognitionQueryContext;
import com.metaforge.agent.cognition.api.spi.CognitionResult;
import com.metaforge.agent.cognition.operator.common.AbstractCognitionOperator;
import com.metaforge.computeengine.api.dto.request.ImpactDiffusionRequest;
import com.metaforge.computeengine.api.enums.TraversalDirection;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class RelationalImpactTraceOperator extends AbstractCognitionOperator {

    @Override
    public String operatorId() {
        return "relational.impact-trace";
    }

    @Override
    public DimensionCategory category() {
        return DimensionCategory.RELATIONAL;
    }

    @Override
    public CognitionResult execute(CognitionQueryContext context) {
        String entityFqn = context.entityFqn();
        if (entityFqn == null || entityFqn.isBlank()) {
            return wrapFailure("缺少 entityFqn 参数");
        }

        int maxDepth = resolveMaxDepth(context);
        String direction = resolveDirection(context);
        Set<com.metaforge.computeengine.api.enums.AssociationType> relationTypes = resolveRelationTypes(context);

        final String fqn = entityFqn;
        Map<String, Object> resultData = new LinkedHashMap<>();

        boolean doForward = !"backward".equalsIgnoreCase(direction);
        boolean doBackward = !"forward".equalsIgnoreCase(direction);

        if (doForward) {
            ImpactDiffusionRequest forward = new ImpactDiffusionRequest(
                    fqn, TraversalDirection.FORWARD, maxDepth, relationTypes);
            Object forwardResult = executeWithPort(() -> computeEngineReadPort.diffuseForward(forward));
            if (forwardResult instanceof CognitionResult cr) return cr;
            resultData.put("forward_diffusion", forwardResult);
        }
        if (doBackward) {
            ImpactDiffusionRequest backward = new ImpactDiffusionRequest(
                    fqn, TraversalDirection.BACKWARD, maxDepth, relationTypes);
            Object backwardResult = executeWithPort(() -> computeEngineReadPort.traceBackward(backward));
            if (backwardResult instanceof CognitionResult cr) return cr;
            resultData.put("backward_trace", backwardResult);
        }
        if (doForward && doBackward) {
            // getImpactPaths 需要 source+target 两个锚点，此处无 target 锚点，
            // 传入 null 会触发 compute-engine validateEntity 失败，故跳过路径详情。
            resultData.put("impact_paths", java.util.Collections.emptyList());
        }

        resultData.put("entityFqn", entityFqn);
        resultData.put("maxDepth", maxDepth);
        resultData.put("direction", direction);

        return CognitionResult.success(operatorId(), category(), resultData);
    }

    private String resolveDirection(CognitionQueryContext context) {
        Map<String, Object> params = context.templateParams();
        if (params != null && params.get("direction") instanceof String dir && !dir.isBlank()) {
            return dir.trim();
        }
        return "both";
    }

    private int resolveMaxDepth(CognitionQueryContext context) {
        Map<String, Object> params = context.templateParams();
        if (params != null) {
            for (String key : new String[]{"max_depth", "maxDepth"}) {
                if (params.containsKey(key)) {
                    try {
                        return Integer.parseInt(params.get(key).toString());
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return 3;
    }

    @SuppressWarnings("unchecked")
    private Set<com.metaforge.computeengine.api.enums.AssociationType> resolveRelationTypes(CognitionQueryContext context) {
        Map<String, Object> params = context.templateParams();
        if (params != null && params.get("relationTypes") instanceof List<?> types) {
            Set<com.metaforge.computeengine.api.enums.AssociationType> set = new java.util.LinkedHashSet<>();
            for (Object t : types) {
                if (t instanceof com.metaforge.computeengine.api.enums.AssociationType at) {
                    set.add(at);
                }
            }
            return set;
        }
        return Set.of();
    }
}
