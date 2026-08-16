package com.metaforge.agent.cognition.operator.common;

import com.metaforge.agent.cognition.api.dto.request.Scope;
import com.metaforge.agent.cognition.api.enums.AgentArchetype;
import com.metaforge.agent.cognition.api.enums.CognitionDepth;
import com.metaforge.agent.cognition.api.enums.DimensionCategory;
import com.metaforge.agent.cognition.api.port.GraphReadPort;
import com.metaforge.agent.cognition.api.port.MetadataReadPort;
import com.metaforge.agent.cognition.api.port.MetamodelReadPort;
import com.metaforge.agent.cognition.api.spi.CognitionQueryContext;
import com.metaforge.agent.cognition.api.spi.CognitionResult;
import com.metaforge.agent.cognition.operator.ontological.OntologicalBundleDiscoveryOperator;
import com.metaforge.agent.cognition.operator.relational.RelationalDirectLinkOperator;
import com.metaforge.agent.cognition.operator.structural.StructuralDomainLocatorOperator;
import com.metaforge.common.dto.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@DisplayName("算子确定性计算测试 (SC-006)")
class OperatorDeterminismTest {

    MetamodelReadPort metamodelReadPort;
    MetadataReadPort metadataReadPort;
    GraphReadPort graphReadPort;

    OntologicalBundleDiscoveryOperator bundleDiscoveryOp;
    StructuralDomainLocatorOperator domainLocatorOp;
    RelationalDirectLinkOperator directLinkOp;

    @BeforeEach
    void setUp() {
        metamodelReadPort = mock(MetamodelReadPort.class);
        metadataReadPort = mock(MetadataReadPort.class);
        graphReadPort = mock(GraphReadPort.class);

        bundleDiscoveryOp = createOp(OntologicalBundleDiscoveryOperator.class);
        domainLocatorOp = createOp(StructuralDomainLocatorOperator.class);
        directLinkOp = createOp(RelationalDirectLinkOperator.class);
    }

    private <T> T createOp(Class<T> clazz) {
        try {
            T op = clazz.getDeclaredConstructor().newInstance();
            ReflectionTestUtils.setField(op, "metamodelReadPort", metamodelReadPort);
            ReflectionTestUtils.setField(op, "metadataReadPort", metadataReadPort);
            ReflectionTestUtils.setField(op, "graphReadPort", graphReadPort);
            return op;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("ONTOLOGICAL BundleDiscovery 执行10次返回一致结果")
    void shouldProduceDeterministicBundleDiscoveryResults() {
        doReturn(new PageResult<>(
                List.<Map<String, Object>>of(
                        Map.of("fqn", "order:1.0.0", "name", "订单域"),
                        Map.of("fqn", "user:2.0.0", "name", "用户域")
                ), 2, 1, 20)).when(metamodelReadPort).listBundles(any());

        CognitionQueryContext ctx = new CognitionQueryContext(
                "DISCOVER", null, DimensionCategory.ONTOLOGICAL, Scope.EMPTY,
                List.of("order:1.0.0"), null, Collections.emptyMap(),
                AgentArchetype.EXECUTION, CognitionDepth.L3, null, 20,
                Collections.emptyMap(), Collections.emptyMap());

        CognitionResult first = null;
        for (int i = 0; i < 10; i++) {
            CognitionResult result = bundleDiscoveryOp.execute(ctx);
            if (first == null) {
                first = result;
            } else {
                assertEquals(first.operatorId(), result.operatorId());
                assertEquals(first.category(), result.category());
                assertEquals(first.success(), result.success());
                assertEquals(first.data(), result.data());
                assertEquals(first.error(), result.error());
            }
        }
    }

    @Test
    @DisplayName("STRUCTURAL DomainLocator 执行10次返回一致结果")
    void shouldProduceDeterministicDomainLocatorResults() {
        doReturn(List.of(
                Map.<String, Object>of("sourceFqn", "L1:业务域", "targetFqn", "L2:order",
                        "relationType", "COMPOSITION")
        )).when(graphReadPort).getInboundRelations("L2:order", "COMPOSITION", null);
        doReturn(List.of()).when(graphReadPort).getInboundRelations("L1:业务域", "COMPOSITION", null);

        CognitionQueryContext ctx = new CognitionQueryContext(
                "ORIENT", null, DimensionCategory.STRUCTURAL, Scope.EMPTY,
                List.of("order:1.0.0"), "L2:order", Collections.emptyMap(),
                AgentArchetype.EXECUTION, CognitionDepth.L3, null, 20,
                Collections.emptyMap(), Collections.emptyMap());

        CognitionResult first = null;
        for (int i = 0; i < 10; i++) {
            CognitionResult result = domainLocatorOp.execute(ctx);
            if (first == null) {
                first = result;
            } else {
                assertEquals(first.data(), result.data());
            }
        }
    }

    @Test
    @DisplayName("RELATIONAL DirectLink 执行10次返回一致结果")
    void shouldProduceDeterministicDirectLinkResults() {
        doReturn(List.of(
                Map.<String, Object>of("fqn", "rel-1", "targetFqn", "order:instance-002",
                        "relationType", "ASSOCIATION")
        )).when(graphReadPort).getOutboundRelations(anyString(), any(), any());
        doReturn(List.of()).when(graphReadPort).getInboundRelations(anyString(), any(), any());

        CognitionQueryContext ctx = new CognitionQueryContext(
                "ORIENT", null, DimensionCategory.RELATIONAL, Scope.EMPTY,
                List.of("order:1.0.0"), "order:instance-001", Collections.emptyMap(),
                AgentArchetype.EXECUTION, CognitionDepth.L3, null, 20,
                Collections.emptyMap(), Collections.emptyMap());

        CognitionResult first = null;
        for (int i = 0; i < 10; i++) {
            CognitionResult result = directLinkOp.execute(ctx);
            if (first == null) {
                first = result;
            } else {
                assertEquals(first.data(), result.data());
            }
        }
    }
}
