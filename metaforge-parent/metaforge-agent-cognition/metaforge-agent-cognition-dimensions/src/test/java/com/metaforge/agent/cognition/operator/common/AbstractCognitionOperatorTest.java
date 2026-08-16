package com.metaforge.agent.cognition.operator.common;

import com.metaforge.agent.cognition.api.dto.request.Scope;
import com.metaforge.agent.cognition.api.enums.DimensionCategory;
import com.metaforge.agent.cognition.api.spi.CognitionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AbstractCognitionOperator 单元测试")
class AbstractCognitionOperatorTest {

    static final String TEST_OPERATOR_ID = "test.operator";
    static final DimensionCategory TEST_CATEGORY = DimensionCategory.ONTOLOGICAL;

    TestOperator operator;

    static class TestOperator extends AbstractCognitionOperator {
        @Override
        public String operatorId() {
            return TEST_OPERATOR_ID;
        }

        @Override
        public DimensionCategory category() {
            return TEST_CATEGORY;
        }

        @Override
        public CognitionResult execute(com.metaforge.agent.cognition.api.spi.CognitionQueryContext context) {
            return CognitionResult.success(operatorId(), category(), null);
        }
    }

    @BeforeEach
    void setUp() {
        operator = new TestOperator();
    }

    @Nested
    @DisplayName("wrapFailure 测试")
    class WrapFailureTests {

        @Test
        @DisplayName("返回 CognitionResult 且 success=false")
        void shouldReturnFailureResult() {
            CognitionResult result = operator.wrapFailure("测试错误");

            assertNotNull(result);
            assertFalse(result.success());
            assertEquals(TEST_OPERATOR_ID, result.operatorId());
            assertEquals(TEST_CATEGORY, result.category());
            assertEquals("测试错误", result.error());
            assertNull(result.data());
        }

        @Test
        @DisplayName("error 信息正确传递")
        void shouldPassErrorInfoCorrectly() {
            String errorMsg = "上游服务不可用";

            CognitionResult result = operator.wrapFailure(errorMsg);

            assertEquals(errorMsg, result.error());
            assertFalse(result.success());
        }
    }

    @Nested
    @DisplayName("executeWithPort 测试")
    class ExecuteWithPortTests {

        @Test
        @DisplayName("正常执行返回 Supplier 结果")
        void shouldReturnSupplierResultOnSuccess() {
            String expectedValue = "success-data";
            Supplier<String> supplier = () -> expectedValue;

            Object result = operator.executeWithPort(supplier);

            assertEquals(expectedValue, result);
        }

        @Test
        @DisplayName("异常时返回 CognitionResult.failure")
        void shouldReturnFailureOnException() {
            RuntimeException exception = new RuntimeException("端口调用失败");
            Supplier<Object> failingSupplier = () -> {
                throw exception;
            };

            Object result = operator.executeWithPort(failingSupplier);

            assertTrue(result instanceof CognitionResult);
            CognitionResult failure = (CognitionResult) result;
            assertFalse(failure.success());
            assertEquals(TEST_OPERATOR_ID, failure.operatorId());
            assertEquals(TEST_CATEGORY, failure.category());
            assertTrue(failure.error().contains("PORT_CALL_FAILED"));
            assertTrue(failure.error().contains("端口调用失败"));
        }

        @Test
        @DisplayName("运行时异常时也同样处理")
        void shouldHandleRuntimeException() {
            Supplier<Object> supplier = () -> {
                throw new IllegalStateException("状态异常");
            };

            Object result = operator.executeWithPort(supplier);

            assertTrue(result instanceof CognitionResult);
            CognitionResult failure = (CognitionResult) result;
            assertFalse(failure.success());
            assertTrue(failure.error().contains("状态异常"));
        }

        @Test
        @DisplayName("不抛出异常到调用方")
        void shouldNotThrowExceptionToCaller() {
            Supplier<Object> supplier = () -> {
                throw new RuntimeException("爆炸");
            };

            assertDoesNotThrow(() -> operator.executeWithPort(supplier));
        }
    }

    @Nested
    @DisplayName("buildLazyNode 测试")
    class BuildLazyNodeTests {

        @Test
        @DisplayName("生成正确的 Map 结构 (有 suggestedNextCall)")
        void shouldBuildCorrectMapWithNextCall() {
            Map<String, Object> data = Map.of("id", "bundle-1", "name", "订单域");
            boolean hasChildren = true;
            String suggestedNextCall = "ontological.package-explorer";

            Map<String, Object> node = operator.buildLazyNode(data, hasChildren, suggestedNextCall);

            assertThat(node).containsKeys("data", "has_children", "suggested_next_call");
            assertEquals(data, node.get("data"));
            assertEquals(hasChildren, node.get("has_children"));
            assertEquals(suggestedNextCall, node.get("suggested_next_call"));
        }

        @Test
        @DisplayName("无 suggestedNextCall 时不包含该 key")
        void shouldNotIncludeNextCallWhenNull() {
            Map<String, Object> data = Map.of("id", "entity-1");
            boolean hasChildren = false;

            Map<String, Object> node = operator.buildLazyNode(data, hasChildren, null);

            assertThat(node).containsKeys("data", "has_children");
            assertThat(node).doesNotContainKey("suggested_next_call");
            assertEquals(data, node.get("data"));
            assertEquals(false, node.get("has_children"));
        }

        @Test
        @DisplayName("使用 LinkedHashMap 保持插入顺序")
        void shouldPreserveInsertionOrder() {
            Map<String, Object> data = Map.of("key", "value");
            Map<String, Object> node = operator.buildLazyNode(data, true, "next.call");

            List<String> keys = new ArrayList<>(node.keySet());
            assertEquals("data", keys.get(0));
            assertEquals("has_children", keys.get(1));
            assertEquals("suggested_next_call", keys.get(2));
        }
    }

    @Nested
    @DisplayName("applyScope 测试")
    class ApplyScopeTests {

        static List<Map<String, Object>> createTestData() {
            List<Map<String, Object>> items = new ArrayList<>();
            items.add(Map.of("fqn", "order:1.0.0", "bundleFqn", "order:1.0.0", "name", "订单"));
            items.add(Map.of("fqn", "user:2.0.0", "bundleFqn", "user:2.0.0", "name", "用户"));
            items.add(Map.of("fqn", "product:1.0.0", "bundleFqn", "product:1.0.0", "name", "商品"));
            return items;
        }

        @Test
        @DisplayName("Scope 为空时返回全量数据")
        void shouldReturnAllWhenScopeEmpty() {
            List<Map<String, Object>> data = createTestData();
            AbstractCognitionOperator.ScopeFilterResult result = operator.applyScope(data, Scope.EMPTY);

            assertEquals(3, result.inScopeItems().size());
            assertTrue(result.skippedFqns().isEmpty());
        }

        @Test
        @DisplayName("Scope 为 null 时返回全量数据")
        void shouldReturnAllWhenScopeNull() {
            List<Map<String, Object>> data = createTestData();
            AbstractCognitionOperator.ScopeFilterResult result = operator.applyScope(data, null);

            assertEquals(3, result.inScopeItems().size());
            assertTrue(result.skippedFqns().isEmpty());
        }

        @Test
        @DisplayName("按 bundles 过滤")
        void shouldFilterByBundles() {
            List<Map<String, Object>> data = createTestData();
            Scope scope = new Scope(List.of("order:1.0.0"), List.of(), List.of(), List.of(), List.of());

            AbstractCognitionOperator.ScopeFilterResult result = operator.applyScope(data, scope);

            assertEquals(1, result.inScopeItems().size());
            assertEquals("order:1.0.0", result.inScopeItems().get(0).get("bundleFqn"));
            assertEquals(2, result.skippedFqns().size());
            assertTrue(result.skippedFqns().contains("user:2.0.0"));
            assertTrue(result.skippedFqns().contains("product:1.0.0"));
        }

        @Test
        @DisplayName("按 packages 过滤")
        void shouldFilterByPackages() {
            List<Map<String, Object>> data = List.of(
                    Map.of("fqn", "pkg-1", "packageFqn", "com.order", "name", "订单包"),
                    Map.of("fqn", "pkg-2", "packageFqn", "com.user", "name", "用户包")
            );
            Scope scope = new Scope(List.of(), List.of("com.order"), List.of(), List.of(), List.of());

            AbstractCognitionOperator.ScopeFilterResult result = operator.applyScope(data, scope);

            assertEquals(1, result.inScopeItems().size());
            assertEquals("com.order", result.inScopeItems().get(0).get("packageFqn"));
            assertEquals(1, result.skippedFqns().size());
            assertEquals("pkg-2", result.skippedFqns().get(0));
        }

        @Test
        @DisplayName("按 entitySchemas 过滤")
        void shouldFilterByEntitySchemas() {
            List<Map<String, Object>> data = List.of(
                    Map.of("fqn", "e1", "entitySchemaFqn", "order:OrderEntity", "name", "订单实体"),
                    Map.of("fqn", "e2", "entitySchemaFqn", "order:UserEntity", "name", "用户实体"),
                    Map.of("fqn", "e3", "entitySchemaFqn", "order:ProductEntity", "name", "商品实体")
            );
            Scope scope = new Scope(List.of(), List.of(), List.of(), List.of(), List.of("order:OrderEntity", "order:ProductEntity"));

            AbstractCognitionOperator.ScopeFilterResult result = operator.applyScope(data, scope);

            assertEquals(2, result.inScopeItems().size());
            assertEquals(1, result.skippedFqns().size());
            assertEquals("e2", result.skippedFqns().get(0));
        }

        @Test
        @DisplayName("多维度组合过滤 (AND 语义)")
        void shouldApplyAndLogicForMultipleDimensions() {
            List<Map<String, Object>> data = List.of(
                    Map.of("fqn", "e1", "bundleFqn", "order:1.0.0", "entitySchemaFqn", "order:OrderEntity", "name", "匹配"),
                    Map.of("fqn", "e2", "bundleFqn", "order:1.0.0", "entitySchemaFqn", "order:UserEntity", "name", "不匹配 schema"),
                    Map.of("fqn", "e3", "bundleFqn", "user:2.0.0", "entitySchemaFqn", "order:OrderEntity", "name", "不匹配 bundle")
            );
            Scope scope = new Scope(List.of("order:1.0.0"), List.of(), List.of(), List.of(), List.of("order:OrderEntity"));

            AbstractCognitionOperator.ScopeFilterResult result = operator.applyScope(data, scope);

            assertEquals(1, result.inScopeItems().size());
            assertEquals("e1", result.inScopeItems().get(0).get("fqn"));
            assertEquals(2, result.skippedFqns().size());
        }

        @Test
        @DisplayName("data 为 null 时返回空结果")
        void shouldReturnEmptyWhenDataNull() {
            AbstractCognitionOperator.ScopeFilterResult result = operator.applyScope(null, Scope.EMPTY);

            assertTrue(result.inScopeItems().isEmpty());
            assertTrue(result.skippedFqns().isEmpty());
        }

        @Test
        @DisplayName("data 为空列表时返回空结果")
        void shouldReturnEmptyWhenDataEmpty() {
            AbstractCognitionOperator.ScopeFilterResult result = operator.applyScope(List.of(), new Scope(List.of("order:1.0.0"), List.of(), List.of(), List.of(), List.of()));

            assertTrue(result.inScopeItems().isEmpty());
            assertTrue(result.skippedFqns().isEmpty());
        }
    }
}
