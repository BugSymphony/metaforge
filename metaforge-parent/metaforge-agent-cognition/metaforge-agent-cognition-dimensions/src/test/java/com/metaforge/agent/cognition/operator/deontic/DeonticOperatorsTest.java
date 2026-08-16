package com.metaforge.agent.cognition.operator.deontic;

import com.metaforge.agent.cognition.api.dto.request.Scope;
import com.metaforge.agent.cognition.api.enums.AgentArchetype;
import com.metaforge.agent.cognition.api.enums.CognitionDepth;
import com.metaforge.agent.cognition.api.enums.DimensionCategory;
import com.metaforge.agent.cognition.api.port.GraphReadPort;
import com.metaforge.agent.cognition.api.port.MetadataReadPort;
import com.metaforge.agent.cognition.api.spi.CognitionQueryContext;
import com.metaforge.agent.cognition.api.spi.CognitionResult;
import com.metaforge.common.dto.PageResult;
import com.metaforge.graph.api.dto.RelationInstanceDto;
import com.metaforge.metadata.api.dto.response.MetadataEntityDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@DisplayName("约束论认知算子 (DEONTIC) 单元测试")
class DeonticOperatorsTest {

    GraphReadPort graphReadPort;
    MetadataReadPort metadataReadPort;

    DeonticRuleListingOperator ruleListingOp;
    DeonticLevelClassifierOperator levelClassifierOp;
    DeonticConditionActionOperator conditionActionOp;

    @BeforeEach
    void setUp() {
        graphReadPort = mock(GraphReadPort.class);
        metadataReadPort = mock(MetadataReadPort.class);

        ruleListingOp = createOp(DeonticRuleListingOperator.class);
        levelClassifierOp = createOp(DeonticLevelClassifierOperator.class);
        conditionActionOp = createOp(DeonticConditionActionOperator.class);
    }

    private <T> T createOp(Class<T> clazz) {
        try {
            T op = clazz.getDeclaredConstructor().newInstance();
            ReflectionTestUtils.setField(op, "graphReadPort", graphReadPort);
            ReflectionTestUtils.setField(op, "metadataReadPort", metadataReadPort);
            return op;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    CognitionQueryContext defaultContext(String entityFqn) {
        return new CognitionQueryContext(
                "GUIDE", null, DimensionCategory.DEONTIC, Scope.EMPTY,
                List.of("metaforge:1.0.0"), entityFqn, Collections.emptyMap(),
                AgentArchetype.EXECUTION, CognitionDepth.L3, null, 20,
                Collections.emptyMap(), Collections.emptyMap());
    }

    private RelationInstanceDto ruleRelation(String fqn, String ruleFqn, String stepFqn) {
        RelationInstanceDto dto = new RelationInstanceDto();
        dto.setFqn(fqn);
        dto.setSourceEntityFqn(ruleFqn);
        dto.setTargetEntityFqn(stepFqn);
        dto.setRelationType("ASSOCIATION_REFERENCE");
        dto.setRelationSchemaFqn("metaforge:1.0.0.agent.RuleAppliesTo");
        return dto;
    }

    private MetadataEntityDto entity(String fqn, Map<String, Object> content) {
        MetadataEntityDto dto = new MetadataEntityDto();
        dto.setFqn(fqn);
        dto.setContent(content);
        return dto;
    }

    @Nested
    @DisplayName("Rule Listing Operator")
    class RuleListingTests {

        @Test
        @DisplayName("按 RuleAppliesTo 关系 schema 入边返回适用于步骤的规则")
        void shouldReturnRulesFromRuleAppliesTo() {
            doReturn(new PageResult<>(List.of(
                    ruleRelation("rel-1", "metaforge:1.0.0.agent.Rule_InventoryAboveZero",
                            "metaforge:1.0.0.agent.Step_CheckInventory"),
                    ruleRelation("rel-2", "metaforge:1.0.0.agent.Rule_48hShipping",
                            "metaforge:1.0.0.agent.Step_CheckInventory")
            ), 2, 1, 100)).when(graphReadPort).multiFilter(any());
            doReturn(entity("metaforge:1.0.0.agent.Rule_InventoryAboveZero", Map.of("constraint_level", "MANDATORY")))
                    .when(metadataReadPort).getByFqn("metaforge:1.0.0.agent.Rule_InventoryAboveZero");
            doReturn(entity("metaforge:1.0.0.agent.Rule_48hShipping", Map.of("constraint_level", "RECOMMENDED")))
                    .when(metadataReadPort).getByFqn("metaforge:1.0.0.agent.Rule_48hShipping");

            CognitionResult result = ruleListingOp.execute(defaultContext("metaforge:1.0.0.agent.Step_CheckInventory"));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            assertEquals(2, ((List<?>) data.get("rules")).size());
        }

        @Test
        @DisplayName("无规则时返回空列表")
        void shouldReturnEmptyWhenNoRules() {
            doReturn(new PageResult<>(Collections.emptyList(), 0, 1, 100))
                    .when(graphReadPort).multiFilter(any());

            CognitionResult result = ruleListingOp.execute(defaultContext("metaforge:1.0.0.agent.Step_CheckInventory"));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            assertTrue(((List<?>) data.get("rules")).isEmpty());
        }
    }

    @Nested
    @DisplayName("Level Classifier Operator")
    class LevelClassifierTests {

        @Test
        @DisplayName("MANDATORY/RECOMMENDED/REFERENCE 分类")
        void shouldClassifyLevel() {
            doReturn(entity("rule-1", Map.of("constraint_level", "MANDATORY")))
                    .when(metadataReadPort).getByFqn("rule-1");

            CognitionResult result = levelClassifierOp.execute(defaultContext("rule-1"));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            assertEquals("MANDATORY", data.get("level"));
        }

        @Test
        @DisplayName("使用 level 字段作为备选")
        void shouldFallbackToLevelField() {
            doReturn(entity("rule-2", Map.of("level", "RECOMMENDED")))
                    .when(metadataReadPort).getByFqn("rule-2");

            CognitionResult result = levelClassifierOp.execute(defaultContext("rule-2"));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            assertEquals("RECOMMENDED", data.get("level"));
        }

        @Test
        @DisplayName("缺失 constraint_level 时默认分类")
        void shouldDefaultClassificationWhenMissing() {
            doReturn(entity("rule-3", Map.of()))
                    .when(metadataReadPort).getByFqn("rule-3");

            CognitionResult result = levelClassifierOp.execute(defaultContext("rule-3"));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            assertEquals("REFERENCE", data.get("level"));
        }
    }

    @Nested
    @DisplayName("Condition Action Operator")
    class ConditionActionTests {

        @Test
        @DisplayName("提取 condition + action 字段")
        void shouldExtractConditionAndAction() {
            doReturn(entity("rule-1", Map.of(
                    "condition", "库存数量 <= 0",
                    "action", "must_trigger_restock"
            ))).when(metadataReadPort).getByFqn("rule-1");

            CognitionResult result = conditionActionOp.execute(defaultContext("rule-1"));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            assertEquals("库存数量 <= 0", data.get("condition"));
            assertEquals("must_trigger_restock", data.get("action"));
        }

        @Test
        @DisplayName("缺失字段返回空")
        void shouldReturnEmptyWhenFieldsMissing() {
            doReturn(entity("rule-2", Map.of()))
                    .when(metadataReadPort).getByFqn("rule-2");

            CognitionResult result = conditionActionOp.execute(defaultContext("rule-2"));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            assertEquals("", data.get("condition"));
            assertEquals("", data.get("action"));
        }
    }
}
