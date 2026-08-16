package com.metaforge.agent.cognition.operator.governance;

import com.metaforge.agent.cognition.api.dto.request.Scope;
import com.metaforge.agent.cognition.api.enums.AgentArchetype;
import com.metaforge.agent.cognition.api.enums.CognitionDepth;
import com.metaforge.agent.cognition.api.enums.DimensionCategory;
import com.metaforge.agent.cognition.api.port.GraphReadPort;
import com.metaforge.agent.cognition.api.port.MetadataReadPort;
import com.metaforge.agent.cognition.api.port.MetamodelReadPort;
import com.metaforge.agent.cognition.api.spi.CognitionQueryContext;
import com.metaforge.agent.cognition.api.spi.CognitionResult;
import com.metaforge.common.dto.PageResult;
import com.metaforge.graph.api.dto.RelationInstanceDto;
import com.metaforge.graph.api.dto.RelationQueryRequest;
import com.metaforge.metadata.api.dto.response.MetadataEntityDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@DisplayName("治理认知算子 (GOVERNANCE) 单元测试")
class GovernanceScopeNarrowingTest {

    private static final String STEP_HAS_NEXT_STEP = "metaforge:1.0.0.agent.StepHasNextStep";
    private static final String STEP_USES_CAPABILITY = "metaforge:1.0.0.agent.StepUsesCapability";
    private static final String RULE_APPLIES_TO = "metaforge:1.0.0.agent.RuleAppliesTo";
    private static final String TASK_HAS_ENTRY_DECISION_STEP = "metaforge:1.0.0.agent.TaskHasEntryDecisionStep";

    GraphReadPort graphReadPort;
    MetadataReadPort metadataReadPort;
    MetamodelReadPort metamodelReadPort;
    GovernanceScopeNarrowingOperator operator;

    @BeforeEach
    void setUp() {
        graphReadPort = mock(GraphReadPort.class);
        metadataReadPort = mock(MetadataReadPort.class);
        metamodelReadPort = mock(MetamodelReadPort.class);
        operator = new GovernanceScopeNarrowingOperator();
        ReflectionTestUtils.setField(operator, "graphReadPort", graphReadPort);
        ReflectionTestUtils.setField(operator, "metadataReadPort", metadataReadPort);
        ReflectionTestUtils.setField(operator, "metamodelReadPort", metamodelReadPort);
    }

    CognitionQueryContext defaultContext(String entryFqn) {
        return new CognitionQueryContext(
                "DELEGATE", null, DimensionCategory.GOVERNANCE, Scope.EMPTY,
                List.of("metaforge:1.0.0"), entryFqn, Map.of(),
                AgentArchetype.EXECUTION, CognitionDepth.L3, null, 20,
                Collections.emptyMap(), Collections.emptyMap());
    }

    private RelationInstanceDto relation(String fqn, String source, String target, String schemaFqn) {
        RelationInstanceDto dto = new RelationInstanceDto();
        dto.setFqn(fqn);
        dto.setSourceEntityFqn(source);
        dto.setTargetEntityFqn(target);
        dto.setRelationSchemaFqn(schemaFqn);
        return dto;
    }

    private MetadataEntityDto entity(String fqn, String schemaFqn) {
        MetadataEntityDto dto = new MetadataEntityDto();
        dto.setFqn(fqn);
        dto.setEntitySchemaFqn(schemaFqn);
        return dto;
    }

    @Test
    @DisplayName("三层收窄: 蓝图→关联实体(能力/规则/决策)→Schema去重")
    void shouldPerformThreeLayerNarrowing() {
        doAnswer(invocation -> {
            RelationQueryRequest request = invocation.getArgument(0);
            List<String> schemas = request.getRelationSchemaFqns();
            if (schemas != null && schemas.contains(STEP_HAS_NEXT_STEP)) {
                // 蓝图相邻：step-entry → step-next（出边）、step-prev → step-entry（入边）
                if (request.getSourceEntityFqns() != null) {
                    return new PageResult<>(List.of(
                            relation("rel-next", "metaforge:1.0.0.agent.Step_CheckInventory",
                                    "metaforge:1.0.0.agent.Step_VerifyStock", STEP_HAS_NEXT_STEP)), 1, 1, 100);
                }
                return new PageResult<>(List.of(
                        relation("rel-prev", "metaforge:1.0.0.agent.Step_ReceiveOrder",
                                "metaforge:1.0.0.agent.Step_CheckInventory", STEP_HAS_NEXT_STEP)), 1, 1, 100);
            }
            if (schemas != null && schemas.contains(STEP_USES_CAPABILITY)) {
                return new PageResult<>(List.of(
                        relation("rel-cap", "metaforge:1.0.0.agent.Step_CheckInventory",
                                "metaforge:1.0.0.agent.Cap_InventoryAPI", STEP_USES_CAPABILITY)), 1, 1, 100);
            }
            if (schemas != null && schemas.contains(RULE_APPLIES_TO)) {
                return new PageResult<>(List.of(
                        relation("rel-rule", "metaforge:1.0.0.agent.Rule_InventoryAboveZero",
                                "metaforge:1.0.0.agent.Step_CheckInventory", RULE_APPLIES_TO)), 1, 1, 100);
            }
            if (schemas != null && schemas.contains(TASK_HAS_ENTRY_DECISION_STEP)) {
                return new PageResult<>(Collections.emptyList(), 0, 1, 100);
            }
            return new PageResult<>(Collections.emptyList(), 0, 1, 100);
        }).when(graphReadPort).multiFilter(any());

        doReturn(entity("metaforge:1.0.0.agent.Step_CheckInventory", "metaforge:1.0.0.agent.ExecutionStep"))
                .when(metadataReadPort).getByFqn("metaforge:1.0.0.agent.Step_CheckInventory");
        doReturn(entity("metaforge:1.0.0.agent.Step_VerifyStock", "metaforge:1.0.0.agent.ExecutionStep"))
                .when(metadataReadPort).getByFqn("metaforge:1.0.0.agent.Step_VerifyStock");
        doReturn(entity("metaforge:1.0.0.agent.Step_ReceiveOrder", "metaforge:1.0.0.agent.ExecutionStep"))
                .when(metadataReadPort).getByFqn("metaforge:1.0.0.agent.Step_ReceiveOrder");
        doReturn(entity("metaforge:1.0.0.agent.Cap_InventoryAPI", "metaforge:1.0.0.agent.Capability"))
                .when(metadataReadPort).getByFqn("metaforge:1.0.0.agent.Cap_InventoryAPI");
        doReturn(entity("metaforge:1.0.0.agent.Rule_InventoryAboveZero", "metaforge:1.0.0.agent.ExecutionRule"))
                .when(metadataReadPort).getByFqn("metaforge:1.0.0.agent.Rule_InventoryAboveZero");

        CognitionResult result = operator.execute(defaultContext("metaforge:1.0.0.agent.Step_CheckInventory"));

        assertTrue(result.success());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.data();
        @SuppressWarnings("unchecked")
        List<String> entityFqns = (List<String>) data.get("entityFqns");
        @SuppressWarnings("unchecked")
        List<String> schemas = (List<String>) data.get("schemas");

        assertTrue(entityFqns.contains("metaforge:1.0.0.agent.Cap_InventoryAPI"));
        assertTrue(entityFqns.contains("metaforge:1.0.0.agent.Rule_InventoryAboveZero"));
        assertTrue(schemas.contains("metaforge:1.0.0.agent.Capability"));
        assertTrue(schemas.contains("metaforge:1.0.0.agent.ExecutionRule"));
    }
}
