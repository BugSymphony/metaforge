package com.metaforge.agent.cognition.operator.relational;

import com.metaforge.agent.cognition.api.dto.request.Scope;
import com.metaforge.agent.cognition.api.enums.AgentArchetype;
import com.metaforge.agent.cognition.api.enums.CognitionDepth;
import com.metaforge.agent.cognition.api.enums.DimensionCategory;
import com.metaforge.agent.cognition.api.port.ComputeEngineReadPort;
import com.metaforge.agent.cognition.api.port.GraphReadPort;
import com.metaforge.agent.cognition.api.spi.CognitionQueryContext;
import com.metaforge.agent.cognition.api.spi.CognitionResult;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@DisplayName("关系论认知算子 (RELATIONAL) 单元测试")
class RelationalOperatorsTest {

    GraphReadPort graphReadPort;
    ComputeEngineReadPort computeEngineReadPort;

    RelationalDirectLinkOperator directLinkOp;
    RelationalNeighborhoodOperator neighborhoodOp;
    RelationalImpactTraceOperator impactTraceOp;

    @BeforeEach
    void setUp() {
        graphReadPort = mock(GraphReadPort.class);
        computeEngineReadPort = mock(ComputeEngineReadPort.class);

        directLinkOp = createOperator(RelationalDirectLinkOperator.class);
        neighborhoodOp = createOperator(RelationalNeighborhoodOperator.class);
        impactTraceOp = createOperator(RelationalImpactTraceOperator.class);
    }

    private <T> T createOperator(Class<T> clazz) {
        try {
            T op = clazz.getDeclaredConstructor().newInstance();
            ReflectionTestUtils.setField(op, "graphReadPort", graphReadPort);
            ReflectionTestUtils.setField(op, "computeEngineReadPort", computeEngineReadPort);
            return op;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    CognitionQueryContext defaultContext(String entityFqn) {
        return new CognitionQueryContext(
                "ORIENT", null, DimensionCategory.RELATIONAL, Scope.EMPTY,
                List.of("order:1.0.0"), entityFqn, Collections.emptyMap(),
                    AgentArchetype.EXECUTION, CognitionDepth.L3, null, 20,
                Collections.emptyMap(), Collections.emptyMap());
    }

    @Nested
    @DisplayName("Direct Link Operator")
    class DirectLinkTests {

        @Test
        @DisplayName("按 AssociationType 分组返回入边+出边")
        void shouldGroupByAssociationType() {
            com.metaforge.graph.api.dto.RelationInstanceDto outbound =
                    new com.metaforge.graph.api.dto.RelationInstanceDto();
            outbound.setFqn("rel-assoc-1");
            outbound.setTargetEntityFqn("order:instance-002");
            outbound.setRelationSchemaFqn("order:OrderUserRelation");
            outbound.setRelationType("ASSOCIATION");
            com.metaforge.graph.api.dto.RelationInstanceDto inbound =
                    new com.metaforge.graph.api.dto.RelationInstanceDto();
            inbound.setFqn("rel-comp-1");
            inbound.setSourceEntityFqn("order:parent");
            inbound.setRelationSchemaFqn("order:OrderComposition");
            inbound.setRelationType("COMPOSITION");

            doReturn(List.of(outbound)).when(graphReadPort)
                    .getOutboundRelations(eq("order:instance-001"), eq(null), eq(null));
            doReturn(List.of(inbound)).when(graphReadPort)
                    .getInboundRelations(eq("order:instance-001"), eq(null), eq(null));

            CognitionResult result = directLinkOp.execute(defaultContext("order:instance-001"));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            assertThat(data).containsKeys("outbound", "inbound");
            assertEquals(1, ((List<?>) data.get("outbound")).size());
            assertEquals(1, ((List<?>) data.get("inbound")).size());
        }
    }

    @Nested
    @DisplayName("Neighborhood Operator")
    class NeighborhoodTests {

        @Test
        @DisplayName("返回 2-degree 邻域实体含间接关系")
        void shouldReturn2DegreeNeighborhood() {
            com.metaforge.computeengine.api.dto.response.GraphQueryResult result = new com.metaforge.computeengine.api.dto.response.GraphQueryResult(
                    List.of(
                            new com.metaforge.computeengine.api.dto.common.EntitySummary("order:instance-001", "订单", "order:OrderEntity"),
                            new com.metaforge.computeengine.api.dto.common.EntitySummary("order:instance-002", "订单2", "order:OrderEntity"),
                            new com.metaforge.computeengine.api.dto.common.EntitySummary("user:user-001", "用户", "user:UserEntity")
                    ),
                    List.of(
                            new com.metaforge.computeengine.api.dto.common.RelationSummary("rel-1", com.metaforge.computeengine.api.enums.AssociationType.ASSOCIATION_REFERENCE, "order:instance-001", "order:instance-002"),
                            new com.metaforge.computeengine.api.dto.common.RelationSummary("rel-2", com.metaforge.computeengine.api.enums.AssociationType.ASSOCIATION_REFERENCE, "order:instance-002", "user:user-001")
                    ),
                    Map.of("order:instance-001", List.of("rel-1")),
                    false, null, null
            );
            doReturn(result).when(computeEngineReadPort).queryAdjacency(any());

            CognitionResult opResult = neighborhoodOp.execute(defaultContext("order:instance-001"));

            assertTrue(opResult.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) opResult.data();
            assertThat(data).containsKeys("entities", "relations", "adjacency_map", "maxDepth");
            assertThat((List<?>) data.get("entities")).hasSize(3);
            assertThat((List<?>) data.get("relations")).hasSize(2);
        }

        @Test
        @DisplayName("默认 maxDepth=2，最大为3")
        void shouldDefaultMaxDepthTo2() {
            doReturn(new com.metaforge.computeengine.api.dto.response.GraphQueryResult(
                    List.of(), List.of(), Map.of(), false, null, null))
                    .when(computeEngineReadPort).queryAdjacency(any());

            CognitionResult opResult = neighborhoodOp.execute(defaultContext("order:instance-001"));
            assertTrue(opResult.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) opResult.data();
            assertEquals(2, data.get("maxDepth"));
        }

        @Test
        @DisplayName("max_depth（snake_case）被解析并透传")
        void shouldResolveSnakeCaseMaxDepth() {
            doReturn(new com.metaforge.computeengine.api.dto.response.GraphQueryResult(
                    List.of(), List.of(), Map.of(), false, null, null))
                    .when(computeEngineReadPort).queryAdjacency(any());

            CognitionQueryContext ctx = new CognitionQueryContext(
                    "FORECAST", null, DimensionCategory.RELATIONAL, Scope.EMPTY,
                    List.of("order:1.0.0"), "order:instance-001", Map.of("max_depth", 3),
                AgentArchetype.EXECUTION, CognitionDepth.L3, null, 20,
                Collections.emptyMap(), Collections.emptyMap());

            neighborhoodOp.execute(ctx);

            org.mockito.ArgumentCaptor<com.metaforge.computeengine.api.dto.request.AdjacencyQueryRequest> captor =
                    org.mockito.ArgumentCaptor.forClass(
                            com.metaforge.computeengine.api.dto.request.AdjacencyQueryRequest.class);
            org.mockito.Mockito.verify(computeEngineReadPort).queryAdjacency(captor.capture());
            assertEquals(3, captor.getValue().maxDepth());
        }
    }

    @Nested
    @DisplayName("Impact Trace Operator")
    class ImpactTraceTests {

        @Test
        @DisplayName("正向扩散返回级联链")
        void shouldReturnForwardDiffusionChain() {
            Map<String, Object> forwardResult = Map.of(
                    "diffusionPath", List.of(
                            Map.of("source", "order:instance-001", "target", "order:instance-002"),
                            Map.of("source", "order:instance-002", "target", "user:user-001")
                    ),
                    "affectedEntities", List.of("order:instance-002", "user:user-001")
            );
            Map<String, Object> backwardResult = Map.of(
                    "upstreamDependencies", List.of("order:parent")
            );
            Map<String, Object> pathResult = Map.of(
                    "paths", List.of(
                            Map.of("path", List.of("order:instance-001", "order:instance-002", "user:user-001"))
                    )
            );
            doReturn(forwardResult).when(computeEngineReadPort).diffuseForward(any());
            doReturn(backwardResult).when(computeEngineReadPort).traceBackward(any());
            doReturn(pathResult).when(computeEngineReadPort).getImpactPaths(anyString(), anyString(), any(), anyInt());

            CognitionResult result = impactTraceOp.execute(defaultContext("order:instance-001"));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            assertThat(data).containsKeys("forward_diffusion", "backward_trace", "impact_paths");
        }

        @Test
        @DisplayName("反向追踪返回上游依赖")
        void shouldReturnUpstreamDependencies() {
            doReturn(Map.of("diffusionPath", List.of())).when(computeEngineReadPort).diffuseForward(any());
            doReturn(Map.of("upstreamDependencies", List.of("order:parent", "product:product-001")))
                    .when(computeEngineReadPort).traceBackward(any());
            doReturn(Map.of("paths", List.of())).when(computeEngineReadPort).getImpactPaths(anyString(), anyString(), any(), anyInt());

            CognitionResult result = impactTraceOp.execute(defaultContext("order:instance-001"));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            @SuppressWarnings("unchecked")
            Map<String, Object> bt = (Map<String, Object>) data.get("backward_trace");
            assertNotNull(bt);
            List<?> deps = (List<?>) bt.get("upstreamDependencies");
            assertEquals(2, deps.size());
        }

        @Test
        @DisplayName("direction=forward 仅返回正向扩散，不调用反向与路径")
        void shouldRespectForwardDirection() {
            doReturn(Map.of("diffusionPath", List.of("a", "b")))
                    .when(computeEngineReadPort).diffuseForward(any());

            CognitionResult result = impactTraceOp.execute(ctxWithParams("order:instance-001",
                    Map.of("direction", "forward")));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            assertThat(data).containsKeys("forward_diffusion");
            assertThat(data).doesNotContainKeys("backward_trace", "impact_paths");
            org.mockito.Mockito.verify(computeEngineReadPort, org.mockito.Mockito.never()).traceBackward(any());
            org.mockito.Mockito.verify(computeEngineReadPort, org.mockito.Mockito.never()).getImpactPaths(anyString(), anyString(), any(), anyInt());
        }

        @Test
        @DisplayName("direction=backward 仅返回反向依赖溯源")
        void shouldRespectBackwardDirection() {
            doReturn(Map.of("upstreamDependencies", List.of("order:parent")))
                    .when(computeEngineReadPort).traceBackward(any());

            CognitionResult result = impactTraceOp.execute(ctxWithParams("order:instance-001",
                    Map.of("direction", "backward")));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            assertThat(data).containsKeys("backward_trace");
            assertThat(data).doesNotContainKeys("forward_diffusion", "impact_paths");
            org.mockito.Mockito.verify(computeEngineReadPort, org.mockito.Mockito.never()).diffuseForward(any());
            org.mockito.Mockito.verify(computeEngineReadPort, org.mockito.Mockito.never()).getImpactPaths(anyString(), anyString(), any(), anyInt());
        }

        @Test
        @DisplayName("direction 缺省为 both，三份结果齐全")
        void shouldDefaultToBothDirections() {
            doReturn(Map.of("diffusionPath", List.of())).when(computeEngineReadPort).diffuseForward(any());
            doReturn(Map.of("upstreamDependencies", List.of())).when(computeEngineReadPort).traceBackward(any());
            doReturn(Map.of("paths", List.of())).when(computeEngineReadPort).getImpactPaths(anyString(), anyString(), any(), anyInt());

            CognitionResult result = impactTraceOp.execute(defaultContext("order:instance-001"));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            assertThat(data).containsKeys("forward_diffusion", "backward_trace", "impact_paths");
        }

        @Test
        @DisplayName("max_depth（snake_case）被解析并传入")
        void shouldResolveSnakeCaseMaxDepth() {
            doReturn(Map.of("diffusionPath", List.of())).when(computeEngineReadPort).diffuseForward(any());
            doReturn(Map.of("upstreamDependencies", List.of())).when(computeEngineReadPort).traceBackward(any());
            doReturn(Map.of("paths", List.of())).when(computeEngineReadPort).getImpactPaths(anyString(), anyString(), any(), anyInt());

            impactTraceOp.execute(ctxWithParams("order:instance-001", Map.of("max_depth", 5)));

            org.mockito.ArgumentCaptor<com.metaforge.computeengine.api.dto.request.ImpactDiffusionRequest> captor =
                    org.mockito.ArgumentCaptor.forClass(
                            com.metaforge.computeengine.api.dto.request.ImpactDiffusionRequest.class);
            org.mockito.Mockito.verify(computeEngineReadPort).diffuseForward(captor.capture());
            assertEquals(5, captor.getValue().maxDepth());
        }

        private CognitionQueryContext ctxWithParams(String entityFqn, Map<String, Object> params) {
            return new CognitionQueryContext(
                    "FORECAST", null, DimensionCategory.RELATIONAL, Scope.EMPTY,
                    List.of("order:1.0.0"), entityFqn, params,
                    AgentArchetype.EXECUTION, CognitionDepth.L3, null, 20,
                    Collections.emptyMap(), Collections.emptyMap());
        }
    }
}
