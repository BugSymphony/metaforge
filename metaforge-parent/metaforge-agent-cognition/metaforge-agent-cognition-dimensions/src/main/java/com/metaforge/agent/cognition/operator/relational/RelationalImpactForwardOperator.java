package com.metaforge.agent.cognition.operator.relational;

import com.metaforge.agent.cognition.api.enums.DimensionCategory;
import com.metaforge.agent.cognition.api.spi.CognitionQueryContext;
import com.metaforge.agent.cognition.api.spi.CognitionResult;
import com.metaforge.agent.cognition.operator.common.AbstractCognitionOperator;
import com.metaforge.computeengine.api.dto.request.ImpactDiffusionRequest;
import com.metaforge.computeengine.api.dto.response.ImpactTraceResult;
import com.metaforge.computeengine.api.enums.AssociationType;
import com.metaforge.computeengine.api.enums.TraversalDirection;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 正向影响扩散——从变更起始实体沿出边 BFS 扩散 N 度，列出受影响实体及规模。
 */
@Component
public class RelationalImpactForwardOperator extends AbstractCognitionOperator {

    @Override
    public String operatorId() {
        return "relational.impact-forward";
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
        Set<AssociationType> relationTypes = resolveRelationTypes(context);

        ImpactDiffusionRequest forward = new ImpactDiffusionRequest(
                entityFqn, TraversalDirection.FORWARD, maxDepth, relationTypes);
        Object forwardResult = executeWithPort(() -> computeEngineReadPort.diffuseForward(forward));
        if (forwardResult instanceof CognitionResult cr) return cr;

        Map<String, Object> resultData = new LinkedHashMap<>();
        resultData.put("direction", "forward");
        resultData.put("entityFqn", entityFqn);
        resultData.put("maxDepth", maxDepth);
        resultData.put("forward_diffusion", forwardResult);
        if (forwardResult instanceof ImpactTraceResult r) {
            resultData.put("count", r.totalImpacted());
        }
        return CognitionResult.success(operatorId(), category(), resultData);
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
    private Set<AssociationType> resolveRelationTypes(CognitionQueryContext context) {
        Map<String, Object> params = context.templateParams();
        if (params != null && params.get("relationTypes") instanceof List<?> types) {
            Set<AssociationType> set = new LinkedHashSet<>();
            for (Object t : types) {
                if (t instanceof AssociationType at) {
                    set.add(at);
                }
            }
            return set;
        }
        return Set.of();
    }
}
