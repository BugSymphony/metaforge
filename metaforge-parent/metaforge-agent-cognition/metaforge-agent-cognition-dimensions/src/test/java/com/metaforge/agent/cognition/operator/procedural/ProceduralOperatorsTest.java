package com.metaforge.agent.cognition.operator.procedural;

import com.metaforge.agent.cognition.api.dto.request.Scope;
import com.metaforge.agent.cognition.api.enums.AgentArchetype;
import com.metaforge.agent.cognition.api.enums.CognitionDepth;
import com.metaforge.agent.cognition.api.enums.DimensionCategory;
import com.metaforge.agent.cognition.api.port.ComputeEngineReadPort;
import com.metaforge.agent.cognition.api.port.GraphReadPort;
import com.metaforge.agent.cognition.api.port.MetadataReadPort;
import com.metaforge.agent.cognition.api.spi.CognitionQueryContext;
import com.metaforge.agent.cognition.api.spi.CognitionResult;
import com.metaforge.common.dto.PageResult;
import com.metaforge.graph.api.dto.RelationInstanceDto;
import com.metaforge.graph.api.dto.RelationQueryRequest;
import com.metaforge.metadata.api.dto.response.MetadataEntityDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@DisplayName("流程论认知算子 (PROCEDURAL) 单元测试")
class ProceduralOperatorsTest {

    private static final String STEP_HAS_NEXT_STEP = "metaforge:1.0.0.agent.StepHasNextStep";

    GraphReadPort graphReadPort;
    ComputeEngineReadPort computeEngineReadPort;
    MetadataReadPort metadataReadPort;

    ProceduralFlowBlueprintOperator flowBlueprintOp;
    ProceduralAdjacentStepOperator adjacentStepOp;
    ProceduralDecisionBranchOperator decisionBranchOp;

    @BeforeEach
    void setUp() {
        graphReadPort = mock(GraphReadPort.class);
        computeEngineReadPort = mock(ComputeEngineReadPort.class);
        metadataReadPort = mock(MetadataReadPort.class);

        flowBlueprintOp = createOp(ProceduralFlowBlueprintOperator.class);
        adjacentStepOp = createOp(ProceduralAdjacentStepOperator.class);
        decisionBranchOp = createOp(ProceduralDecisionBranchOperator.class);
    }

    private <T> T createOp(Class<T> clazz) {
        try {
            T op = clazz.getDeclaredConstructor().newInstance();
            ReflectionTestUtils.setField(op, "graphReadPort", graphReadPort);
            ReflectionTestUtils.setField(op, "computeEngineReadPort", computeEngineReadPort);
            ReflectionTestUtils.setField(op, "metadataReadPort", metadataReadPort);
            return op;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    CognitionQueryContext defaultContext(String entityFqn) {
        return new CognitionQueryContext(
                "GUIDE", null, DimensionCategory.PROCEDURAL, Scope.EMPTY,
                List.of("metaforge:1.0.0"), entityFqn, Collections.emptyMap(),
                AgentArchetype.EXECUTION, CognitionDepth.L3, null, 20,
                Collections.emptyMap(), Collections.emptyMap());
    }

    private RelationInstanceDto relation(String fqn, String source, String target, String schemaFqn) {
        RelationInstanceDto dto = new RelationInstanceDto();
        dto.setFqn(fqn);
        dto.setSourceEntityFqn(source);
        dto.setTargetEntityFqn(target);
        dto.setRelationType("PROCESS_SEQUENCE");
        dto.setRelationSchemaFqn(schemaFqn);
        return dto;
    }

    @Nested
    @DisplayName("Flow Blueprint Operator")
    class FlowBlueprintTests {

        @Test
        @DisplayName("返回带 ENTRY/EXIT 标记的步骤序列")
        void shouldReturnPathWithMarkers() {
            doAnswer(invocation -> {
                RelationQueryRequest request = invocation.getArgument(0);
                String source = request.getSourceEntityFqns() == null ? null : request.getSourceEntityFqns().get(0);
                if ("step-1".equals(source)) {
                    return new PageResult<>(List.of(relation("r1", "step-1", "step-2", STEP_HAS_NEXT_STEP)), 1, 1, 100);
                }
                if ("step-2".equals(source)) {
                    return new PageResult<>(List.of(relation("r2", "step-2", "step-3", STEP_HAS_NEXT_STEP)), 1, 1, 100);
                }
                return new PageResult<>(Collections.emptyList(), 0, 1, 100);
            }).when(graphReadPort).multiFilter(any());

            CognitionResult result = flowBlueprintOp.execute(defaultContext("step-1"));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            assertThat(data).containsKeys("annotated_path", "length", "entryStepFqn");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> annotated = (List<Map<String, Object>>) data.get("annotated_path");
            assertEquals(3, annotated.size());
            assertEquals("ENTRY", annotated.get(0).get("marker"));
            assertEquals("EXIT", annotated.get(2).get("marker"));
        }

        @Test
        @DisplayName("Task 锚点经 entry_step_fqn 解析入口步")
        void shouldResolveEntryStepForTask() {
            MetadataEntityDto task = new MetadataEntityDto();
            task.setFqn("metaforge:1.0.0.agent.Task_InventoryCheck");
            task.setEntitySchemaFqn("metaforge:1.0.0.agent.Task");
            task.setContent(Map.of("entry_step_fqn", "metaforge:1.0.0.agent.Step_CheckInventory"));
            doReturn(task).when(metadataReadPort).getByFqn("metaforge:1.0.0.agent.Task_InventoryCheck");
            doReturn(new PageResult<>(Collections.emptyList(), 0, 1, 100))
                    .when(graphReadPort).multiFilter(any());

            CognitionResult result = flowBlueprintOp.execute(defaultContext("metaforge:1.0.0.agent.Task_InventoryCheck"));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            assertEquals("metaforge:1.0.0.agent.Step_CheckInventory", data.get("entryStepFqn"));
            assertEquals(1, ((List<?>) data.get("annotated_path")).size());
        }
    }

    @Nested
    @DisplayName("Adjacent Step Operator")
    class AdjacentStepTests {

        @Test
        @DisplayName("prev/next 按 StepHasNextStep 关系 schema 返回")
        void shouldReturnPrevAndNextSteps() {
            doAnswer(invocation -> {
                RelationQueryRequest request = invocation.getArgument(0);
                List<RelationInstanceDto> content = request.getSourceEntityFqns() != null
                        ? List.of(relation("rel-next", "step-2", "step-3", STEP_HAS_NEXT_STEP))
                        : List.of(relation("rel-prev", "step-1", "step-2", STEP_HAS_NEXT_STEP));
                return new PageResult<>(content, content.size(), 1, 100);
            }).when(graphReadPort).multiFilter(any());

            CognitionResult result = adjacentStepOp.execute(defaultContext("step-2"));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            assertThat(data).containsKeys("previous", "next", "current");
            assertEquals("step-3", ((Map<?, ?>) ((List<?>) data.get("next")).get(0)).get("targetEntityFqn"));
            assertEquals("step-1", ((Map<?, ?>) ((List<?>) data.get("previous")).get(0)).get("sourceEntityFqn"));
        }

        @Test
        @DisplayName("无前驱或后继时返回空")
        void shouldReturnEmptyWhenNoAdjacent() {
            doReturn(new PageResult<>(Collections.emptyList(), 0, 1, 100))
                    .when(graphReadPort).multiFilter(any());

            CognitionResult result = adjacentStepOp.execute(defaultContext("orphan-step"));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            assertEquals(0, ((List<?>) data.get("previous")).size());
            assertEquals(0, ((List<?>) data.get("next")).size());
        }
    }

    @Nested
    @DisplayName("Decision Branch Operator")
    class DecisionBranchTests {

        @Test
        @DisplayName(">1 StepHasNextStep 出边识别为决策分支")
        void shouldIdentifyDecisionBranchWithMultipleOutbound() {
            RelationInstanceDto branchA = relation("rel-a", "step-decision", "branch-a", STEP_HAS_NEXT_STEP);
            branchA.setContent(Map.of("condition", "amount>1000"));
            RelationInstanceDto branchB = relation("rel-b", "step-decision", "branch-b", STEP_HAS_NEXT_STEP);
            branchB.setContent(Map.of("condition", "amount<=1000"));
            doAnswer(invocation -> {
                RelationQueryRequest request = invocation.getArgument(0);
                List<String> schemas = request.getRelationSchemaFqns();
                if (schemas != null && schemas.contains(STEP_HAS_NEXT_STEP)) {
                    return new PageResult<>(List.of(branchA, branchB), 2, 1, 100);
                }
                return new PageResult<>(Collections.emptyList(), 0, 1, 100);
            }).when(graphReadPort).multiFilter(any());
            doReturn(Map.of("affectedEntities", List.of("branch-a-result")))
                    .when(computeEngineReadPort).diffuseForward(any());

            CognitionResult result = decisionBranchOp.execute(defaultContext("step-decision"));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            assertTrue((Boolean) data.get("isDecisionBranch"));
            assertEquals(2, ((List<?>) data.get("branches")).size());
        }

        @Test
        @DisplayName("单出边则非决策分支")
        void shouldNotIdentifyAsDecisionWithSingleOutbound() {
            doAnswer(invocation -> {
                RelationQueryRequest request = invocation.getArgument(0);
                List<String> schemas = request.getRelationSchemaFqns();
                if (schemas != null && schemas.contains(STEP_HAS_NEXT_STEP)) {
                    return new PageResult<>(List.of(
                            relation("rel-next", "step-1", "next-step", STEP_HAS_NEXT_STEP)), 1, 1, 100);
                }
                return new PageResult<>(Collections.emptyList(), 0, 1, 100);
            }).when(graphReadPort).multiFilter(any());

            CognitionResult result = decisionBranchOp.execute(defaultContext("step-1"));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            assertEquals(false, data.get("isDecisionBranch"));
        }

        @Test
        @DisplayName("DecisionStep 本体：输出决策内容 + 任务上下文")
        void shouldReturnDecisionContentForDecisionStep() {
            String ds = "metaforge:1.0.0.agent.DecisionStep_RiskCheck";
            MetadataEntityDto decision = new MetadataEntityDto();
            decision.setFqn(ds);
            decision.setName("风控决策");
            decision.setEntitySchemaFqn("metaforge:1.0.0.agent.DecisionStep");
            decision.setContent(Map.of(
                    "condition_expression", "风控结论是否为 HIGH",
                    "recommended_option", "拦截支付",
                    "rationale", "高风险交易需人工复核"));
            doReturn(decision).when(metadataReadPort).getByFqn(ds);

            doAnswer(invocation -> {
                RelationQueryRequest request = invocation.getArgument(0);
                List<String> schemas = request.getRelationSchemaFqns();
                if (schemas != null
                        && schemas.contains("metaforge:1.0.0.agent.TaskHasEntryDecisionStep")
                        && request.getTargetEntityFqns() != null) {
                    return new PageResult<>(List.of(relation(
                            "rel-pay-to-decision", "task-payment", ds,
                            "metaforge:1.0.0.agent.TaskHasEntryDecisionStep")), 1, 1, 100);
                }
                return new PageResult<>(Collections.emptyList(), 0, 1, 100);
            }).when(graphReadPort).multiFilter(any());

            CognitionResult result = decisionBranchOp.execute(defaultContext(ds));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            assertEquals("metaforge:1.0.0.agent.DecisionStep", data.get("targetType"));
            assertEquals("task-payment", data.get("taskFqn"));
            @SuppressWarnings("unchecked")
            Map<String, Object> current = (Map<String, Object>) data.get("current");
            assertEquals("拦截支付", current.get("recommended_option"));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> decisionSteps = (List<Map<String, Object>>) data.get("decisionSteps");
            assertEquals(0, decisionSteps.size());
        }

        @Test
        @DisplayName("ExecutionStep 的 StepHasNextDecisionStep 出边识别为下游决策")
        void shouldResolveDownstreamDecisionForExecutionStep() {
            String ds = "metaforge:1.0.0.agent.DecisionStep_DemoGate1";
            MetadataEntityDto decision = new MetadataEntityDto();
            decision.setFqn(ds);
            decision.setName("演示决策步骤1");
            decision.setEntitySchemaFqn("metaforge:1.0.0.agent.DecisionStep");
            decision.setContent(Map.of("condition_expression", "是否进入下一级审批"));
            doReturn(decision).when(metadataReadPort).getByFqn(ds);

            doAnswer(invocation -> {
                RelationQueryRequest request = invocation.getArgument(0);
                List<String> schemas = request.getRelationSchemaFqns();
                if (schemas != null
                        && schemas.contains("metaforge:1.0.0.agent.StepHasNextDecisionStep")) {
                    return new PageResult<>(List.of(relation(
                            "rel-work-to-gate", "step-work", ds,
                            "metaforge:1.0.0.agent.StepHasNextDecisionStep")), 1, 1, 100);
                }
                return new PageResult<>(Collections.emptyList(), 0, 1, 100);
            }).when(graphReadPort).multiFilter(any());

            CognitionResult result = decisionBranchOp.execute(defaultContext("step-work"));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> downstream = (List<Map<String, Object>>) data.get("downstreamDecision");
            assertEquals(1, downstream.size());
            assertEquals(ds, downstream.get(0).get("fqn"));
        }
    }
}
