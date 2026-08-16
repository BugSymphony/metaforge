package com.metaforge.agent.cognition.operator.ontological;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@DisplayName("本体论认知算子链 (ONTOLOGICAL) 单元测试")
class OntologicalOperatorsTest {

    MetamodelReadPort metamodelReadPort;
    MetadataReadPort metadataReadPort;
    GraphReadPort graphReadPort;

    OntologicalBundleDiscoveryOperator bundleDiscoveryOp;
    OntologicalPackageExplorerOperator packageExplorerOp;
    OntologicalEntitySchemaInventoryOperator entitySchemaInventoryOp;
    OntologicalRelationSchemaInventoryOperator relationSchemaInventoryOp;
    OntologicalDomainDrillDownOperator domainDrillDownOp;
    OntologicalInstanceCatalogOperator instanceCatalogOp;
    OntologicalEntityProfileOperator entityProfileOp;

    @BeforeEach
    void setUp() {
        metamodelReadPort = mock(MetamodelReadPort.class);
        metadataReadPort = mock(MetadataReadPort.class);
        graphReadPort = mock(GraphReadPort.class);

        bundleDiscoveryOp = createOperator(OntologicalBundleDiscoveryOperator.class);
        packageExplorerOp = createOperator(OntologicalPackageExplorerOperator.class);
        entitySchemaInventoryOp = createOperator(OntologicalEntitySchemaInventoryOperator.class);
        relationSchemaInventoryOp = createOperator(OntologicalRelationSchemaInventoryOperator.class);
        domainDrillDownOp = createOperator(OntologicalDomainDrillDownOperator.class);
        instanceCatalogOp = createOperator(OntologicalInstanceCatalogOperator.class);
        entityProfileOp = createOperator(OntologicalEntityProfileOperator.class);
    }

    private <T> T createOperator(Class<T> clazz) {
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

    CognitionQueryContext defaultContext() {
        return defaultContext(Scope.EMPTY);
    }

    CognitionQueryContext defaultContext(Scope scope) {
        return new CognitionQueryContext(
                "DISCOVER",
                null,
                DimensionCategory.ONTOLOGICAL,
                scope,
                List.of("order:1.0.0", "user:2.0.0"),
                null,
                Collections.emptyMap(),
                AgentArchetype.EXECUTION,
                CognitionDepth.L3,
                null,
                20,
                Collections.emptyMap(),
                Collections.emptyMap()
        );
    }

    @Nested
    @DisplayName("Bundle Discovery Operator")
    class BundleDiscoveryTests {

        @Test
        @DisplayName("正常返回 Bundle 列表的 lazy 节点")
        void shouldReturnLazyBundleNodes() {
            doReturn(new PageResult<>(
                    List.<Map<String, Object>>of(
                            Map.of("fqn", "order:1.0.0", "name", "订单域"),
                            Map.of("fqn", "user:2.0.0", "name", "用户域")
                    ), 2, 1, 20))
                    .when(metamodelReadPort).listBundles(any());

            CognitionResult result = bundleDiscoveryOp.execute(defaultContext());

            assertTrue(result.success());
            assertNotNull(result.data());

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> nodes = (List<Map<String, Object>>) result.data();
            assertEquals(2, nodes.size());

            Map<String, Object> firstNode = nodes.get(0);
            assertThat(firstNode).containsKeys("data", "has_children", "suggested_next_call");
            assertEquals("ontological.package-explorer", firstNode.get("suggested_next_call"));
        }

        @Test
        @DisplayName("DTO 形态（BundleDto）正常转换并产出 lazy 节点")
        void shouldHandleBundleDto() {
            com.metaforge.metamodel.api.dto.response.BundleDto dto =
                    new com.metaforge.metamodel.api.dto.response.BundleDto();
            dto.setFqn("order:1.0.0");
            dto.setName("订单域");
            doReturn(new PageResult<>(List.of(dto), 1, 1, 20))
                    .when(metamodelReadPort).listBundles(any());

            CognitionResult result = bundleDiscoveryOp.execute(defaultContext());

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> nodes = (List<Map<String, Object>>) result.data();
            assertEquals(1, nodes.size());
            @SuppressWarnings("unchecked")
            Map<String, Object> bundleData = (Map<String, Object>) nodes.get(0).get("data");
            assertEquals("order:1.0.0", bundleData.get("fqn"));
            assertEquals("订单域", bundleData.get("name"));
        }
    }

    @Nested
    @DisplayName("Package Explorer Operator")
    class PackageExplorerTests {

        @Test
        @DisplayName("返回 Package 的 lazy 节点")
        void shouldReturnLazyPackageNodes() {
            doReturn(List.of(
                    Map.<String, Object>of("fqn", "com.order.entity", "name", "实体包"),
                    Map.<String, Object>of("fqn", "com.order.relation", "name", "关系包")
            )).when(metamodelReadPort).listPackages(anyString());
            doReturn(List.of()).when(graphReadPort).getOutboundRelations(anyString(), anyString(), any());

            CognitionQueryContext ctx = new CognitionQueryContext(
                    "DISCOVER", null, DimensionCategory.ONTOLOGICAL, Scope.EMPTY,
                    List.of("order:1.0.0"), "order:1.0.0",
                    Collections.singletonMap("bundleVersionFqn", "order:1.0.0"),
                    AgentArchetype.EXECUTION, CognitionDepth.L3, null, 20,
                Collections.emptyMap(), Collections.emptyMap());

            CognitionResult result = packageExplorerOp.execute(ctx);

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> nodes = (List<Map<String, Object>>) result.data();
            assertEquals(2, nodes.size());
            assertThat(nodes.get(0)).containsKeys("data", "has_children");
        }
    }

    @Nested
    @DisplayName("Entity Schema Inventory Operator")
    class EntitySchemaInventoryTests {

        @Test
        @DisplayName("返回带 instance_count 的 lazy 节点")
        void shouldReturnSchemaNodesWithInstanceCount() {
            doReturn(new PageResult<>(
                    List.<Map<String, Object>>of(
                            Map.of("fqn", "order:OrderEntity", "name", "订单实体", "key_attributes", List.of("orderId", "customerId")),
                            Map.of("fqn", "order:OrderItem", "name", "订单项", "key_attributes", List.of("itemId"))
                    ), 2, 1, 20))
                    .when(metamodelReadPort).listEntitySchemas(any());
            doReturn(new PageResult<>(List.of(), 50, 1, 20))
                    .when(metadataReadPort).listByEntitySchema(anyString(), any());

            CognitionResult result = entitySchemaInventoryOp.execute(defaultContext());

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> nodes = (List<Map<String, Object>>) result.data();
            assertEquals(2, nodes.size());
            assertThat(nodes.get(0)).containsKeys("data", "has_children", "suggested_next_call", "instance_count", "key_attributes");
        }

        @Test
        @DisplayName("构造真实 ElementQueryRequest（含 parent_fqn 锚点与分页）")
        void shouldBuildElementQueryRequest() {
            doReturn(new PageResult<>(
                    List.<Map<String, Object>>of(Map.of("fqn", "order:OrderEntity")), 1, 1, 20))
                    .when(metamodelReadPort).listEntitySchemas(any());
            doReturn(new PageResult<>(List.of(), 0, 1, 1))
                    .when(metadataReadPort).listByEntitySchema(anyString(), any());

            CognitionQueryContext ctx = new CognitionQueryContext(
                    "DISCOVER", "ontological.entity-schema-inventory", DimensionCategory.ONTOLOGICAL,
                    Scope.EMPTY, List.of("order:1.0.0"), null,
                    Map.of("level", "EntitySchema", "parent_fqn", "order:1.0.0.order-core"),
                    AgentArchetype.EXECUTION, CognitionDepth.L3, 3, 50,
                Collections.emptyMap(), Collections.emptyMap());

            entitySchemaInventoryOp.execute(ctx);

            org.mockito.ArgumentCaptor<com.metaforge.metamodel.api.dto.ElementQueryRequest> captor =
                    org.mockito.ArgumentCaptor.forClass(com.metaforge.metamodel.api.dto.ElementQueryRequest.class);
            org.mockito.Mockito.verify(metamodelReadPort).listEntitySchemas(captor.capture());
            assertEquals(List.of("order:1.0.0.order-core"), captor.getValue().getFqnPrefixes());
            assertEquals(3, captor.getValue().getPage());
            assertEquals(50, captor.getValue().getSize());
        }
    }

    @Nested
    @DisplayName("Relation Schema Inventory Operator")
    class RelationSchemaInventoryTests {

        @Test
        @DisplayName("返回 RelationSchema 的 lazy 节点")
        void shouldReturnRelationSchemaNodes() {
            doReturn(new PageResult<>(
                    List.<Map<String, Object>>of(
                            Map.of("fqn", "order:OrderUserRelation", "name", "订单用户关系"),
                            Map.of("fqn", "order:OrderProductRelation", "name", "订单商品关系")
                    ), 2, 1, 20))
                    .when(metamodelReadPort).listRelationSchemas(any());

            CognitionResult result = relationSchemaInventoryOp.execute(defaultContext());

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> nodes = (List<Map<String, Object>>) result.data();
            assertEquals(2, nodes.size());
            assertThat(nodes.get(0)).containsKeys("data", "has_children");
        }
    }

    @Nested
    @DisplayName("Domain DrillDown Operator")
    class DomainDrillDownTests {

        private Map<String, Object> configWithAliases() {
            return Map.of("levelAliases", Map.of(
                    "L1", "metaforge:1.0.0.common.SubjectDomainGroup",
                    "L2", "metaforge:1.0.0.common.SubjectDomain",
                    "Task", "metaforge:1.0.0.agent.Task",
                    "Agent", "metaforge:1.0.0.agent.Agent"));
        }

        private CognitionQueryContext drillCtx(Map<String, Object> params, Map<String, Object> config) {
            return new CognitionQueryContext(
                    "ORIENT", null, DimensionCategory.ONTOLOGICAL, Scope.EMPTY,
                    List.of("metaforge:1.0.0"), null, params,
                    AgentArchetype.EXECUTION, CognitionDepth.L3, null, 20, config,
                    Collections.emptyMap()
            );
        }

        private com.metaforge.metadata.api.dto.response.MetadataEntityDto entityDto(
                String fqn, String schemaFqn, String name) {
            com.metaforge.metadata.api.dto.response.MetadataEntityDto dto =
                    new com.metaforge.metadata.api.dto.response.MetadataEntityDto();
            dto.setFqn(fqn);
            dto.setEntitySchemaFqn(schemaFqn);
            dto.setName(name);
            dto.setContent(Map.of());
            return dto;
        }

        private com.metaforge.graph.api.dto.RelationInstanceDto relationEdge(String source, String target) {
            com.metaforge.graph.api.dto.RelationInstanceDto dto =
                    new com.metaforge.graph.api.dto.RelationInstanceDto();
            dto.setSourceEntityFqn(source);
            dto.setTargetEntityFqn(target);
            dto.setRelationType("COMPOSITION");
            return dto;
        }

        @Test
        @DisplayName("level=null 顶层自动发现默认查 L1（SubjectDomainGroup）")
        void shouldAutoDiscoverWhenLevelNull() {
            doReturn(new PageResult<>(
                    List.of(
                            entityDto("metaforge:1.0.0.common.Group_Order",
                                    "metaforge:1.0.0.common.SubjectDomainGroup", "订单域组")
                    ), 1, 1, 20))
                    .when(metadataReadPort).listByEntitySchema(
                            eq("metaforge:1.0.0.common.SubjectDomainGroup"), any());
            doReturn(List.of()).when(graphReadPort).getOutboundRelations(anyString(), anyString(), any());

            CognitionResult result = domainDrillDownOp.execute(drillCtx(Map.of(), configWithAliases()));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            assertThat(data).containsKeys("children_grouped");
            @SuppressWarnings("unchecked")
            Map<String, List<Map<String, Object>>> grouped =
                    (Map<String, List<Map<String, Object>>>) data.get("children_grouped");
            // 顶层自动发现默认仅 L1（SubjectDomainGroup）
            assertThat(grouped.keySet()).containsExactly("metaforge:1.0.0.common.SubjectDomainGroup");
        }

        @Test
        @DisplayName("level=L2 别名解析为 SubjectDomain 并按 COMPOSITION 子节点过滤")
        void shouldResolveL2Alias() {
            doReturn(List.of(
                    relationEdge("metaforge:1.0.0.common.Group_Order", "metaforge:1.0.0.common.Domain_Inventory"),
                    relationEdge("metaforge:1.0.0.common.Group_Order", "metaforge:1.0.0.common.Domain_Finance")
            )).when(graphReadPort).getOutboundRelations(eq("metaforge:1.0.0.common.Group_Order"),
                    eq("COMPOSITION"), eq(null));
            doReturn(entityDto("metaforge:1.0.0.common.Domain_Inventory",
                    "metaforge:1.0.0.common.SubjectDomain", "库存域"))
                    .when(metadataReadPort).getByFqn("metaforge:1.0.0.common.Domain_Inventory");
            doReturn(entityDto("metaforge:1.0.0.common.Domain_Finance",
                    "metaforge:1.0.0.common.SubjectDomain", "财务域"))
                    .when(metadataReadPort).getByFqn("metaforge:1.0.0.common.Domain_Finance");

            CognitionResult result = domainDrillDownOp.execute(drillCtx(Map.of(
                    "parent_fqn", "metaforge:1.0.0.common.Group_Order",
                    "level", "L2"), configWithAliases()));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            @SuppressWarnings("unchecked")
            Map<String, List<Map<String, Object>>> grouped =
                    (Map<String, List<Map<String, Object>>>) data.get("children_grouped");
            // 两个子节点均 enrich 出 SubjectDomain 类型，过滤后归入该分组
            assertThat(grouped.keySet()).containsExactly("metaforge:1.0.0.common.SubjectDomain");
            assertThat(grouped.get("metaforge:1.0.0.common.SubjectDomain")).hasSize(2);
        }

        @Test
        @DisplayName("level=Task 类型别名 → 直接按类型查询 Task 实体")
        void shouldResolveTaskAliasWithFilter() {
            doReturn(new PageResult<>(
                    List.of(
                            entityDto("metaforge:1.0.0.agent.Task_Check",
                                    "metaforge:1.0.0.agent.Task", "盘点任务")
                    ), 1, 1, 20))
                    .when(metadataReadPort).listByEntitySchema(eq("metaforge:1.0.0.agent.Task"), any());
            doReturn(List.of()).when(graphReadPort).getOutboundRelations(anyString(), anyString(), any());

            CognitionResult result = domainDrillDownOp.execute(drillCtx(Map.of(
                    "level", "Task"), configWithAliases()));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            @SuppressWarnings("unchecked")
            Map<String, List<Map<String, Object>>> grouped =
                    (Map<String, List<Map<String, Object>>>) data.get("children_grouped");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tasks = grouped.get("metaforge:1.0.0.agent.Task");
            assertThat(tasks).hasSize(1);
            assertThat(tasks.get(0).get("fqn")).isEqualTo("metaforge:1.0.0.agent.Task_Check");
        }

        @Test
        @DisplayName("level=完整 EntitySchemaFQN 跨 Bundle 精确过滤")
        void shouldUseFullFqnAcrossBundle() {
            doReturn(List.of(
                    relationEdge("codebase:1.0.0.Module_Auth", "codebase:1.0.0.structure.Class_Order"),
                    relationEdge("codebase:1.0.0.Module_Auth", "codebase:1.0.0.structure.Class_User")
            )).when(graphReadPort).getOutboundRelations(eq("codebase:1.0.0.Module_Auth"),
                    eq("COMPOSITION"), eq(null));
            doReturn(entityDto("codebase:1.0.0.structure.Class_Order",
                    "codebase:1.0.0.structure.Class", "Order 类"))
                    .when(metadataReadPort).getByFqn("codebase:1.0.0.structure.Class_Order");
            doReturn(entityDto("codebase:1.0.0.structure.Class_User",
                    "codebase:1.0.0.structure.Class", "User 类"))
                    .when(metadataReadPort).getByFqn("codebase:1.0.0.structure.Class_User");

            CognitionResult result = domainDrillDownOp.execute(drillCtx(Map.of(
                    "parent_fqn", "codebase:1.0.0.Module_Auth",
                    "level", "codebase:1.0.0.structure.Class"), configWithAliases()));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            @SuppressWarnings("unchecked")
            Map<String, List<Map<String, Object>>> grouped =
                    (Map<String, List<Map<String, Object>>>) data.get("children_grouped");
            assertThat(grouped.keySet()).containsExactly("codebase:1.0.0.structure.Class");
            assertThat(grouped.get("codebase:1.0.0.structure.Class")).hasSize(2);
        }

        @Test
        @DisplayName("未知 level 返回 InvalidLevelException(34013)")
        void shouldFailOnUnknownLevel() {
            assertThatThrownBy(() -> domainDrillDownOp.execute(drillCtx(Map.of(
                    "level", "UNKNOWN"), configWithAliases())))
                    .isInstanceOf(com.metaforge.agent.cognition.api.exception.InvalidLevelException.class);
        }

        @Test
        @DisplayName("无 levelAliases 配置时未知别名返回失败")
        void shouldFailWhenNoAliasConfig() {
            assertThatThrownBy(() -> domainDrillDownOp.execute(drillCtx(Map.of(
                    "level", "L2"), Collections.emptyMap())))
                    .isInstanceOf(com.metaforge.agent.cognition.api.exception.InvalidLevelException.class);
        }
    }

    @Nested
    @DisplayName("Instance Catalog Operator")
    class InstanceCatalogTests {

        @Test
        @DisplayName("返回分页实例列表 (full mode)")
        void shouldReturnPaginatedInstances() {
            doReturn(new PageResult<>(
                    List.<Map<String, Object>>of(
                            Map.of("fqn", "order:instance-001", "orderId", "ORD-001", "customerId", "CUST-001"),
                            Map.of("fqn", "order:instance-002", "orderId", "ORD-002", "customerId", "CUST-002")
                    ), 50, 1, 20))
                    .when(metadataReadPort).listByEntitySchema(anyString(), any());

            CognitionQueryContext ctx = new CognitionQueryContext(
                    "DISCOVER", null, DimensionCategory.ONTOLOGICAL, Scope.EMPTY,
                    List.of("order:1.0.0"), null,
                    Map.of("entitySchemaFqn", "order:OrderEntity"),
                    AgentArchetype.EXECUTION, CognitionDepth.L3, null, 10,
                Collections.emptyMap(), Collections.emptyMap());

            CognitionResult result = instanceCatalogOp.execute(ctx);

            assertTrue(result.success());
            assertNotNull(result.data());
        }
    }

    @Nested
    @DisplayName("Entity Profile Operator")
    class EntityProfileTests {

        @Test
        @DisplayName("返回完整实体画像含 domain_location")
        void shouldReturnFullEntityProfileWithDomainLocation() {
            com.metaforge.metadata.api.dto.response.MetadataEntityDto entity =
                    new com.metaforge.metadata.api.dto.response.MetadataEntityDto();
            entity.setFqn("order:instance-001");
            entity.setEntitySchemaFqn("order:OrderEntity");
            entity.setContent(Map.of(
                    "orderId", "ORD-001",
                    "customerId", "CUST-001",
                    "amount", "1000.00",
                    "status", "已创建"
            ));
            Map<String, Object> entitySchema = Map.of(
                    "fqn", "order:OrderEntity",
                    "name", "订单实体",
                    "attributes", List.of(
                            Map.of("name", "orderId", "type", "string"),
                            Map.of("name", "customerId", "type", "string"),
                            Map.of("name", "amount", "type", "decimal"),
                            Map.of("name", "status", "type", "string")
                    )
            );

            doReturn(entity).when(metadataReadPort).getByFqn("order:instance-001");
            doReturn(entitySchema).when(metamodelReadPort).getEntitySchema("order:OrderEntity");
            com.metaforge.graph.api.dto.RelationInstanceDto parentEdge =
                    new com.metaforge.graph.api.dto.RelationInstanceDto();
            parentEdge.setSourceEntityFqn("order:parent-entity");
            parentEdge.setTargetEntityFqn("order:instance-001");
            doReturn(List.of(parentEdge))
                    .when(graphReadPort).getInboundRelations("order:instance-001", "COMPOSITION", null);
            doReturn(List.of())
                    .when(graphReadPort).getInboundRelations("order:parent-entity", "COMPOSITION", null);

            CognitionQueryContext ctx = new CognitionQueryContext(
                    "DISCOVER", null, DimensionCategory.ONTOLOGICAL, Scope.EMPTY,
                    List.of("order:1.0.0"), "order:instance-001",
                    Map.of("entitySchemaFqn", "order:OrderEntity"),
                    AgentArchetype.EXECUTION, CognitionDepth.L3, null, 20,
                    Collections.emptyMap(), Collections.emptyMap());

            CognitionResult result = entityProfileOp.execute(ctx);

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> profile = (Map<String, Object>) result.data();
            assertThat(profile).containsKeys("entity", "entitySchema", "domain_location");
            assertEquals("order:instance-001", ((Map<?, ?>) profile.get("entity")).get("fqn"));
            assertEquals("ORD-001", ((Map<?, ?>) profile.get("entity")).get("orderId"));
            assertEquals(entitySchema, profile.get("entitySchema"));
            assertThat(profile.get("domain_location"))
                    .asList()
                    .containsExactly("order:parent-entity", "order:instance-001");
        }
    }

    @Nested
    @DisplayName("完整发现链测试")
    class FullChainTests {

        @Test
        @DisplayName("bundle-discovery → package-explorer → entity-schema-inventory → relation-schema-inventory 全链")
        void shouldExecuteFullDiscoveryChain() {
            doReturn(new PageResult<>(List.<Map<String, Object>>of(Map.of("fqn", "order:1.0.0", "name", "订单域")), 1, 1, 20))
                    .when(metamodelReadPort).listBundles(any());
            doReturn(List.of(Map.<String, Object>of("fqn", "com.order.entity", "name", "实体包")))
                    .when(metamodelReadPort).listPackages(anyString());
            doReturn(List.of()).when(graphReadPort).getOutboundRelations(anyString(), anyString(), any());
            doReturn(new PageResult<>(List.<Map<String, Object>>of(Map.of("fqn", "order:OrderEntity", "name", "订单实体", "key_attributes", List.of("orderId"))), 1, 1, 20))
                    .when(metamodelReadPort).listEntitySchemas(any());
            doReturn(new PageResult<>(List.of(), 50, 1, 20))
                    .when(metadataReadPort).listByEntitySchema(anyString(), any());
            doReturn(new PageResult<>(List.<Map<String, Object>>of(Map.of("fqn", "order:OrderUserRelation", "name", "订单用户关系")), 1, 1, 20))
                    .when(metamodelReadPort).listRelationSchemas(any());

            CognitionResult r1 = bundleDiscoveryOp.execute(defaultContext());
            assertTrue(r1.success());

            CognitionQueryContext ctx2 = new CognitionQueryContext(
                    "DISCOVER", null, DimensionCategory.ONTOLOGICAL, Scope.EMPTY,
                    List.of("order:1.0.0"), "order:1.0.0",
                    Map.of("bundleVersionFqn", "order:1.0.0"),
                    AgentArchetype.EXECUTION, CognitionDepth.L3, null, 20,
                Collections.emptyMap(), Collections.emptyMap());
            CognitionResult r2 = packageExplorerOp.execute(ctx2);
            assertTrue(r2.success());

            CognitionResult r3 = entitySchemaInventoryOp.execute(defaultContext());
            assertTrue(r3.success());

            CognitionResult r4 = relationSchemaInventoryOp.execute(defaultContext());
            assertTrue(r4.success());
        }
    }

    @Nested
    @DisplayName("Scope 过滤与边界情况")
    class ScopeAndEdgeCases {

        @Test
        @DisplayName("scope.bundles 过滤排除不在范围内的 Bundle")
        void shouldFilterByScopeBundles() {
            doReturn(new PageResult<>(
                    List.<Map<String, Object>>of(
                            Map.of("fqn", "order:1.0.0", "name", "订单域"),
                            Map.of("fqn", "user:2.0.0", "name", "用户域"),
                            Map.of("fqn", "product:1.0.0", "name", "商品域")
                    ), 3, 1, 20))
                    .when(metamodelReadPort).listBundles(any());

            Scope scope = new Scope(List.of("order:1.0.0"), List.of(), List.of(), List.of(), List.of());
            CognitionResult result = bundleDiscoveryOp.execute(defaultContext(scope));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> nodes = (List<Map<String, Object>>) result.data();
            assertEquals(1, nodes.size());
            @SuppressWarnings("unchecked")
            Map<String, Object> bundleData = (Map<String, Object>) nodes.get(0).get("data");
            assertEquals("order:1.0.0", bundleData.get("fqn"));
        }

        @Test
        @DisplayName("未知 Bundle FQN 返回空列表")
        void shouldReturnEmptyForUnknownBundle() {
            doReturn(new PageResult<>(List.of(), 0, 1, 20))
                    .when(metamodelReadPort).listBundles(any());

            CognitionResult result = bundleDiscoveryOp.execute(defaultContext());

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            List<?> nodes = (List<?>) result.data();
            assertTrue(nodes.isEmpty());
        }

        @Test
        @DisplayName("entity-profile 中未知 FQN 返回失败")
        void shouldReturnFailureForUnknownEntityFqn() {
            doReturn(null).when(metadataReadPort).getByFqn("unknown:entity");

            CognitionQueryContext ctx = new CognitionQueryContext(
                    "DISCOVER", null, DimensionCategory.ONTOLOGICAL, Scope.EMPTY,
                    List.of("order:1.0.0"), "unknown:entity",
                    Map.of("entitySchemaFqn", "order:OrderEntity"),
                    AgentArchetype.EXECUTION, CognitionDepth.L3, null, 20,
                Collections.emptyMap(), Collections.emptyMap());

            CognitionResult result = entityProfileOp.execute(ctx);

            assertFalse(result.success());
            assertNotNull(result.error());
        }
    }
}
