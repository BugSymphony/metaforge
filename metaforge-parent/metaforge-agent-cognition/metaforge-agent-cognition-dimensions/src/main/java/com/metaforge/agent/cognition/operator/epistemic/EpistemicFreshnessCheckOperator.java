package com.metaforge.agent.cognition.operator.epistemic;

import com.metaforge.agent.cognition.api.enums.DimensionCategory;
import com.metaforge.agent.cognition.api.spi.CognitionQueryContext;
import com.metaforge.agent.cognition.api.spi.CognitionResult;
import com.metaforge.agent.cognition.operator.common.AbstractCognitionOperator;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class EpistemicFreshnessCheckOperator extends AbstractCognitionOperator {

    @Override
    public String operatorId() {
        return "epistemic.freshness-check";
    }

    @Override
    public DimensionCategory category() {
        return DimensionCategory.EPISTEMIC;
    }

    @Override
    public CognitionResult execute(CognitionQueryContext context) {
        Map<String, Object> params = context.templateParams();
        boolean hasVersionAnchors = params != null && params.containsKey("version_anchors")
                && params.get("version_anchors") instanceof Map<?, ?> anchors && !anchors.isEmpty();

        Map<String, Object> resultData = new LinkedHashMap<>();
        resultData.put("fresh", hasVersionAnchors);
        resultData.put("stale", !hasVersionAnchors);

        if (hasVersionAnchors) {
            resultData.put("version_anchors", params.get("version_anchors"));
        } else {
            resultData.put("stale_reason", "version_anchors 缺失或为空");
        }

        return CognitionResult.success(operatorId(), category(), resultData);
    }
}
