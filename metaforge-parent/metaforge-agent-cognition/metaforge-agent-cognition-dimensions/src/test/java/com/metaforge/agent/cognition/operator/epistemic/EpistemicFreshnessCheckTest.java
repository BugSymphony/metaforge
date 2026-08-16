package com.metaforge.agent.cognition.operator.epistemic;

import com.metaforge.agent.cognition.api.dto.request.Scope;
import com.metaforge.agent.cognition.api.enums.AgentArchetype;
import com.metaforge.agent.cognition.api.enums.CognitionDepth;
import com.metaforge.agent.cognition.api.enums.DimensionCategory;
import com.metaforge.agent.cognition.api.spi.CognitionQueryContext;
import com.metaforge.agent.cognition.api.spi.CognitionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("认知论认知算子 (EPISTEMIC) 单元测试")
class EpistemicFreshnessCheckTest {

    EpistemicFreshnessCheckOperator operator;

    @BeforeEach
    void setUp() {
        operator = new EpistemicFreshnessCheckOperator();
    }

    CognitionQueryContext contextWithVersionAnchors(Map<String, Object> versionAnchors) {
        return new CognitionQueryContext(
                "ORIENT", null, DimensionCategory.EPISTEMIC, Scope.EMPTY,
                List.of("order:1.0.0"), null,
                Map.of("version_anchors", versionAnchors),
                AgentArchetype.EXECUTION, CognitionDepth.L3, null, 20,
                Collections.emptyMap(), Collections.emptyMap());
    }

    @Test
    @DisplayName("version_anchors 与缓存一致时返回 fresh")
    void shouldReturnFreshWhenVersionsMatch() {
        Map<String, Object> versionAnchors = Map.of(
                "order:1.0.0", "1.0.0",
                "user:2.0.0", "2.0.0"
        );

        CognitionResult result = operator.execute(contextWithVersionAnchors(versionAnchors));

        assertTrue(result.success());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.data();
        assertTrue((Boolean) data.get("fresh"));
        assertFalse((Boolean) data.get("stale"));
    }

    @Test
    @DisplayName("无 version_anchors 时标记为 stale")
    void shouldMarkStaleWhenNoVersionAnchors() {
        CognitionQueryContext ctx = new CognitionQueryContext(
                "ORIENT", null, DimensionCategory.EPISTEMIC, Scope.EMPTY,
                List.of("order:1.0.0"), null, Map.of(),
                AgentArchetype.EXECUTION, CognitionDepth.L3, null, 20,
                Collections.emptyMap(), Collections.emptyMap());

        CognitionResult result = operator.execute(ctx);

        assertTrue(result.success());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.data();
        assertTrue((Boolean) data.get("stale"));
    }
}
