package com.metaforge.agent.cognition.operator.capability;

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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@DisplayName("能力论认知算子 (CAPABILITY) 单元测试")
class CapabilityOperatorsTest {

    private static final String CAP_SCHEMA = "metaforge:1.0.0.agent.Capability";
    private static final String STEP_SCHEMA = "metaforge:1.0.0.agent.ExecutionStep";

    GraphReadPort graphReadPort;
    MetadataReadPort metadataReadPort;

    CapabilityToolDiscoveryOperator toolDiscoveryOp;
    CapabilityCallMethodOperator callMethodOp;
    CapabilityProtocolDetailOperator protocolDetailOp;

    @BeforeEach
    void setUp() {
        graphReadPort = mock(GraphReadPort.class);
        metadataReadPort = mock(MetadataReadPort.class);

        toolDiscoveryOp = createOp(CapabilityToolDiscoveryOperator.class);
        callMethodOp = createOp(CapabilityCallMethodOperator.class);
        protocolDetailOp = createOp(CapabilityProtocolDetailOperator.class);
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
        return defaultContext(entityFqn, Collections.emptyMap());
    }

    CognitionQueryContext defaultContext(String entityFqn, Map<String, Object> operatorConfig) {
        return new CognitionQueryContext(
                "GUIDE", null, DimensionCategory.CAPABILITY, Scope.EMPTY,
                List.of("metaforge:1.0.0"), entityFqn, Collections.emptyMap(),
                AgentArchetype.EXECUTION, CognitionDepth.L3, null, 20,
                Collections.emptyMap(), operatorConfig);
    }

    private RelationInstanceDto relation(String fqn, String source, String target, String schemaFqn) {
        RelationInstanceDto dto = new RelationInstanceDto();
        dto.setFqn(fqn);
        dto.setSourceEntityFqn(source);
        dto.setTargetEntityFqn(target);
        dto.setRelationType("ASSOCIATION_REFERENCE");
        dto.setRelationSchemaFqn(schemaFqn);
        return dto;
    }

    private MetadataEntityDto entity(String fqn, String schemaFqn, String name, String description,
                                     Map<String, Object> content) {
        MetadataEntityDto dto = new MetadataEntityDto();
        dto.setFqn(fqn);
        dto.setEntitySchemaFqn(schemaFqn);
        dto.setName(name);
        dto.setDescription(description);
        dto.setContent(content);
        return dto;
    }

    /** 按 RelationQueryRequest 的 source/target 区分 outbound/inbound，返回 PageResult。 */
    private void stubMultiFilter(List<RelationInstanceDto> outbound, List<RelationInstanceDto> inbound) {
        doAnswer(invocation -> {
            RelationQueryRequest request = invocation.getArgument(0);
            List<RelationInstanceDto> content =
                    request.getSourceEntityFqns() != null ? outbound : inbound;
            return new PageResult<>(content, content.size(), 1, 100);
        }).when(graphReadPort).multiFilter(any());
    }

    @Nested
    @DisplayName("Tool Discovery Operator")
    class ToolDiscoveryTests {

        @Test
        @DisplayName("按使用方→Capability 关系 schema 返回能力分层摘要（不含 interface_spec）")
        void shouldReturnLayeredSummariesFromCapabilityRelations() {
            stubMultiFilter(
                    List.of(relation("rel-step-uses", "metaforge:1.0.0.agent.Step_CheckInventory",
                            "metaforge:1.0.0.agent.Cap_InventoryAPI",
                            "metaforge:1.0.0.agent.StepUsesCapability")),
                    Collections.emptyList());
            doReturn(entity("metaforge:1.0.0.agent.Cap_InventoryAPI", CAP_SCHEMA,
                    "库存查询API", "查询库存", Map.of("call_method", "REST", "interface_spec", Map.of("endpoint", "x"))))
                    .when(metadataReadPort).getByFqn("metaforge:1.0.0.agent.Cap_InventoryAPI");

            CognitionResult result = toolDiscoveryOp.execute(defaultContext("metaforge:1.0.0.agent.Step_CheckInventory"));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            assertEquals(1, ((List<?>) data.get("capabilities")).size());

            @SuppressWarnings("unchecked")
            Map<String, Object> summary = (Map<String, Object>) ((List<?>) data.get("capabilities")).get(0);
            assertThat(summary).containsKeys("fqn", "name", "description", "protocolType");
            assertEquals("metaforge:1.0.0.agent.Cap_InventoryAPI", summary.get("fqn"));
            assertEquals("Http", summary.get("protocolType"));
            assertThat(summary).doesNotContainKey("interface_spec");
        }

        @Test
        @DisplayName("无能力关系时返回空列表")
        void shouldReturnEmptyWhenNoCapabilities() {
            stubMultiFilter(Collections.emptyList(), Collections.emptyList());

            CognitionResult result = toolDiscoveryOp.execute(defaultContext("metaforge:1.0.0.agent.Step_CheckInventory"));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            assertTrue(((List<?>) data.get("capabilities")).isEmpty());
        }
    }

    @Nested
    @DisplayName("Call Method Operator")
    class CallMethodTests {

        @Test
        @DisplayName("识别 REST 调用方式")
        void shouldIdentifyRestMethod() {
            doReturn(entity("metaforge:1.0.0.agent.Cap_InventoryAPI", CAP_SCHEMA, "库存API", "",
                    Map.of("call_method", "REST")))
                    .when(metadataReadPort).getByFqn("metaforge:1.0.0.agent.Cap_InventoryAPI");

            CognitionResult result = callMethodOp.execute(defaultContext("metaforge:1.0.0.agent.Cap_InventoryAPI"));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            assertEquals("REST", data.get("callMethod"));
        }

        @Test
        @DisplayName("识别 MCP 调用方式")
        void shouldIdentifyMcpMethod() {
            doReturn(entity("metaforge:1.0.0.agent.Cap_RestockQueue", CAP_SCHEMA, "补货队列", "",
                    Map.of("call_method", "MCP")))
                    .when(metadataReadPort).getByFqn("metaforge:1.0.0.agent.Cap_RestockQueue");

            CognitionResult result = callMethodOp.execute(defaultContext("metaforge:1.0.0.agent.Cap_RestockQueue"));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            assertEquals("MCP", data.get("callMethod"));
        }
    }

    @Nested
    @DisplayName("Protocol Detail Operator")
    class ProtocolDetailTests {

        @Test
        @DisplayName("Http 协议解析出 protocol{type:Http, endpoint, method, input/output_schema}")
        void shouldExpandProtocolDetailForHttp() {
            Map<String, Object> interfaceSpec = Map.of(
                    "type", "Http",
                    "endpoint", "https://api.example.com/notify",
                    "method", "POST",
                    "headers", Map.of("Content-Type", "application/json"),
                    "input_schema", Map.of("type", "object"),
                    "output_schema", Map.of("type", "object")
            );
            doReturn(entity("metaforge:1.0.0.agent.Cap_InventoryAPI", CAP_SCHEMA, "库存API", "",
                    Map.of("interface_spec", interfaceSpec)))
                    .when(metadataReadPort).getByFqn("metaforge:1.0.0.agent.Cap_InventoryAPI");
            stubMultiFilter(Collections.emptyList(), Collections.emptyList());

            CognitionResult result = protocolDetailOp.execute(defaultContext("metaforge:1.0.0.agent.Cap_InventoryAPI"));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            assertThat(data).containsKeys("protocol", "interface_spec", "protocol_subtypes");

            @SuppressWarnings("unchecked")
            Map<String, Object> protocol = (Map<String, Object>) data.get("protocol");
            assertEquals("Http", protocol.get("type"));
            assertEquals("https://api.example.com/notify", protocol.get("endpoint"));
            assertEquals("POST", protocol.get("method"));
            assertThat(protocol).containsKeys("headers", "input_schema", "output_schema");
        }

        @Test
        @DisplayName("按协议前缀查询 Capability 引用的协议实例")
        void shouldResolveProtocolInstancesViaPrefix() {
            doReturn(entity("metaforge:1.0.0.agent.Cap_InventoryAPI", CAP_SCHEMA, "库存API", "",
                    Map.of("interface_spec", Map.of("type", "Http", "endpoint", "https://x", "method", "GET"))))
                    .when(metadataReadPort).getByFqn("metaforge:1.0.0.agent.Cap_InventoryAPI");
            stubMultiFilter(
                    List.of(relation("rel-impl", "metaforge:1.0.0.agent.Cap_InventoryAPI",
                            "metaforge:1.0.0.protocol.Http_InventoryQuery",
                            "metaforge:1.0.0.protocol.CapabilityImplementsHttp")),
                    Collections.emptyList());
            doReturn(entity("metaforge:1.0.0.protocol.Http_InventoryQuery",
                    "metaforge:1.0.0.protocol.Http", "库存查询接口", "",
                    Map.of("endpoint", "https://inventory.example.com/api/stock", "method", "GET")))
                    .when(metadataReadPort).getByFqn("metaforge:1.0.0.protocol.Http_InventoryQuery");

            CognitionResult result = protocolDetailOp.execute(defaultContext("metaforge:1.0.0.agent.Cap_InventoryAPI"));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> instances = (List<Map<String, Object>>) data.get("protocol_subtypes");
            assertEquals(1, instances.size());
            assertEquals("metaforge:1.0.0.protocol.Http_InventoryQuery", instances.get(0).get("fqn"));
            assertEquals("metaforge:1.0.0.protocol.Http", instances.get(0).get("entitySchemaFqn"));
        }

        @Test
        @DisplayName("McpTool 协议解析出 server_name + arguments_schema")
        void shouldExpandMcpToolProtocol() {
            Map<String, Object> interfaceSpec = Map.of(
                    "type", "McpTool",
                    "server_name", "inventory-server",
                    "tool_name", "check_stock",
                    "arguments_schema", Map.of("type", "object", "properties", Map.of("sku", Map.of("type", "string")))
            );
            doReturn(entity("metaforge:1.0.0.agent.Cap_RestockQueue", CAP_SCHEMA, "补货队列", "",
                    Map.of("interface_spec", interfaceSpec)))
                    .when(metadataReadPort).getByFqn("metaforge:1.0.0.agent.Cap_RestockQueue");
            stubMultiFilter(Collections.emptyList(), Collections.emptyList());

            CognitionResult result = protocolDetailOp.execute(defaultContext("metaforge:1.0.0.agent.Cap_RestockQueue"));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            @SuppressWarnings("unchecked")
            Map<String, Object> protocol = (Map<String, Object>) data.get("protocol");
            assertEquals("McpTool", protocol.get("type"));
            assertEquals("inventory-server", protocol.get("server_name"));
            assertEquals("check_stock", protocol.get("tool_name"));
            assertThat(protocol).containsKey("arguments_schema");
        }

        @Test
        @DisplayName("Cli 协议解析出 command")
        void shouldExpandCliProtocol() {
            Map<String, Object> interfaceSpec = Map.of(
                    "type", "Cli",
                    "command", "inventory check --sku {sku}"
            );
            doReturn(entity("metaforge:1.0.0.agent.Cap_Cli", CAP_SCHEMA, "CLI能力", "",
                    Map.of("interface_spec", interfaceSpec)))
                    .when(metadataReadPort).getByFqn("metaforge:1.0.0.agent.Cap_Cli");
            stubMultiFilter(Collections.emptyList(), Collections.emptyList());

            CognitionResult result = protocolDetailOp.execute(defaultContext("metaforge:1.0.0.agent.Cap_Cli"));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            @SuppressWarnings("unchecked")
            Map<String, Object> protocol = (Map<String, Object>) data.get("protocol");
            assertEquals("Cli", protocol.get("type"));
            assertEquals("inventory check --sku {sku}", protocol.get("command"));
        }

        @Test
        @DisplayName("LocalMethod 协议解析出 method_ref")
        void shouldExpandLocalMethodProtocol() {
            Map<String, Object> interfaceSpec = Map.of(
                    "type", "LocalMethod",
                    "method_ref", "InventoryService.checkStock"
            );
            doReturn(entity("metaforge:1.0.0.agent.Cap_Local", CAP_SCHEMA, "本地方法", "",
                    Map.of("interface_spec", interfaceSpec)))
                    .when(metadataReadPort).getByFqn("metaforge:1.0.0.agent.Cap_Local");
            stubMultiFilter(Collections.emptyList(), Collections.emptyList());

            CognitionResult result = protocolDetailOp.execute(defaultContext("metaforge:1.0.0.agent.Cap_Local"));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            @SuppressWarnings("unchecked")
            Map<String, Object> protocol = (Map<String, Object>) data.get("protocol");
            assertEquals("LocalMethod", protocol.get("type"));
            assertEquals("InventoryService.checkStock", protocol.get("method_ref"));
        }

        @Test
        @DisplayName("无 interface_spec 时 protocol 为 null")
        void shouldReturnNullWhenNoInterfaceSpec() {
            doReturn(entity("metaforge:1.0.0.agent.Cap_Empty", CAP_SCHEMA, "空能力", "", Map.of()))
                    .when(metadataReadPort).getByFqn("metaforge:1.0.0.agent.Cap_Empty");
            stubMultiFilter(Collections.emptyList(), Collections.emptyList());

            CognitionResult result = protocolDetailOp.execute(defaultContext("metaforge:1.0.0.agent.Cap_Empty"));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            assertNull(data.get("protocol"));
            assertNull(data.get("interface_spec"));
            assertEquals(0, ((List<?>) data.get("protocol_subtypes")).size());
        }

        @Test
        @DisplayName("未声明 type 时按字段特征推断协议类型")
        void shouldInferTypeFromFields() {
            Map<String, Object> interfaceSpec = Map.of(
                    "endpoint", "https://api.example.com/x",
                    "method", "GET"
            );
            doReturn(entity("metaforge:1.0.0.agent.Cap_Infer", CAP_SCHEMA, "推断能力", "",
                    Map.of("interface_spec", interfaceSpec)))
                    .when(metadataReadPort).getByFqn("metaforge:1.0.0.agent.Cap_Infer");
            stubMultiFilter(Collections.emptyList(), Collections.emptyList());

            CognitionResult result = protocolDetailOp.execute(defaultContext("metaforge:1.0.0.agent.Cap_Infer"));

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            @SuppressWarnings("unchecked")
            Map<String, Object> protocol = (Map<String, Object>) data.get("protocol");
            assertEquals("Http", protocol.get("type"));
            assertEquals("https://api.example.com/x", protocol.get("endpoint"));
        }
    }
}
