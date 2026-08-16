package com.metaforge.agent.cognition.operator.structural;

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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@DisplayName("结构论认知算子 (STRUCTURAL) 单元测试")
class StructuralOperatorsTest {

    ComputeEngineReadPort computeEngineReadPort;
    GraphReadPort graphReadPort;

    StructuralDecompositionOperator decompositionOp;
    StructuralBelongingOperator belongingOp;
    StructuralDomainLocatorOperator domainLocatorOp;

    @BeforeEach
    void setUp() {
        computeEngineReadPort = mock(ComputeEngineReadPort.class);
        graphReadPort = mock(GraphReadPort.class);

        decompositionOp = createOperator(StructuralDecompositionOperator.class);
        belongingOp = createOperator(StructuralBelongingOperator.class);
        domainLocatorOp = createOperator(StructuralDomainLocatorOperator.class);
    }

    private <T> T createOperator(Class<T> clazz) {
        try {
            T op = clazz.getDeclaredConstructor().newInstance();
            ReflectionTestUtils.setField(op, "computeEngineReadPort", computeEngineReadPort);
            ReflectionTestUtils.setField(op, "graphReadPort", graphReadPort);
            return op;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    CognitionQueryContext defaultContext(String entityFqn) {
        return new CognitionQueryContext(
                "ORIENT", null, DimensionCategory.STRUCTURAL, Scope.EMPTY,
                List.of("order:1.0.0"), entityFqn, Collections.emptyMap(),
                AgentArchetype.EXECUTION, CognitionDepth.L3, null, 20,
                Collections.emptyMap(), Collections.emptyMap());
    }

    @Nested
    @DisplayName("Decomposition Operator (FORWARD)")
    class DecompositionTests {

        @Test
        @DisplayName("沿 COMPOSITION FORWARD 返回完整子树")
        void shouldReturnFullCompositionSubtreeForward() {
            Map<String, Object> tree = Map.of(
                    "root", Map.of("fqn", "order:instance-001", "name", "订单根"),
                    "children", List.of(
                            Map.of("fqn", "order:item-001", "name", "订单项1"),
                            Map.of("fqn", "order:item-002", "name", "订单项2")
                    )
            );
            doReturn(tree).when(computeEngineReadPort).queryCompositionTree(any());

            CognitionResult result = decompositionOp.execute(defaultContext("order:instance-001"));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            assertNotNull(data);
            assertThat(data).containsKeys("root", "children", "has_children");
            assertEquals(tree.get("root"), data.get("root"));
            assertEquals(tree.get("children"), data.get("children"));
            assertEquals(true, data.get("has_children"));
        }
    }

    @Nested
    @DisplayName("Belonging Operator (BACKWARD)")
    class BelongingTests {

        @Test
        @DisplayName("沿 COMPOSITION BACKWARD 返回父链到根")
        void shouldReturnParentChainToRootBackward() {
            Map<String, Object> chain = Map.of(
                    "root", Map.of("fqn", "L1:业务域/order", "name", "订单业务域"),
                    "path", List.of(
                            Map.of("fqn", "order:parent", "name", "父实体"),
                            Map.of("fqn", "order:instance-001", "name", "订单根")
                    )
            );
            doReturn(chain).when(computeEngineReadPort).queryCompositionTree(any());

            CognitionResult result = belongingOp.execute(defaultContext("order:instance-001"));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            assertNotNull(data);
            assertEquals(chain, data);
        }
    }

    @Nested
    @DisplayName("Domain Locator Operator")
    class DomainLocatorTests {

        @Test
        @DisplayName("返回 L1→L5 路径坐标")
        void shouldReturnL1ToL5PathCoordinates() {
            com.metaforge.graph.api.dto.RelationInstanceDto edge1 =
                    new com.metaforge.graph.api.dto.RelationInstanceDto();
            edge1.setSourceEntityFqn("L2:业务域/order");
            edge1.setTargetEntityFqn("order:instance-001");
            edge1.setRelationType("COMPOSITION");
            com.metaforge.graph.api.dto.RelationInstanceDto edge2 =
                    new com.metaforge.graph.api.dto.RelationInstanceDto();
            edge2.setSourceEntityFqn("L1:业务域");
            edge2.setTargetEntityFqn("L2:业务域/order");
            edge2.setRelationType("COMPOSITION");

            doReturn(List.of(edge1)).when(graphReadPort)
                    .getInboundRelations("order:instance-001", "COMPOSITION", null);
            doReturn(List.of(edge2)).when(graphReadPort)
                    .getInboundRelations("L2:业务域/order", "COMPOSITION", null);
            doReturn(List.of()).when(graphReadPort).getInboundRelations("L1:业务域", "COMPOSITION", null);

            CognitionResult result = domainLocatorOp.execute(defaultContext("order:instance-001"));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            assertThat(data).containsKeys("domain_location", "levels");
            assertThat((List<?>) data.get("domain_location"))
                    .isEqualTo(List.of("L1:业务域", "L2:业务域/order", "order:instance-001"));
        }

        @Test
        @DisplayName("无 COMPOSITION 入边时返回仅含自身")
        void shouldReturnOnlySelfWhenNoCompositionInbound() {
            doReturn(List.of()).when(graphReadPort).getInboundRelations(anyString(), anyString(), any());

            CognitionResult result = domainLocatorOp.execute(defaultContext("orphan-entity"));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            List<?> path = (List<?>) data.get("domain_location");
            assertEquals(1, path.size());
            assertEquals("orphan-entity", path.get(0));
        }
    }
}
