package com.metaforge.agent.cognition.operator.common;

import com.metaforge.agent.cognition.api.enums.DimensionCategory;
import com.metaforge.agent.cognition.api.port.MetadataReadPort;
import com.metaforge.agent.cognition.api.spi.CognitionQueryContext;
import com.metaforge.agent.cognition.api.spi.CognitionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("算子超时测试 (FR-010)")
class OperatorTimeoutTest {

    static class TimedOperator extends AbstractCognitionOperator {
        @Override
        public String operatorId() {
            return "test.timed";
        }

        @Override
        public DimensionCategory category() {
            return DimensionCategory.ONTOLOGICAL;
        }

        @Override
        public CognitionResult execute(CognitionQueryContext context) {
            Object result = executeWithPort(() -> {
                throw new RuntimeException("超时模拟异常");
            });
            if (result instanceof CognitionResult cr) {
                return cr;
            }
            return CognitionResult.success(operatorId(), category(), result);
        }
    }

    TimedOperator operator;

    @BeforeEach
    void setUp() {
        operator = new TimedOperator();
        operator.metamodelReadPort = mock(com.metaforge.agent.cognition.api.port.MetamodelReadPort.class);
        operator.metadataReadPort = mock(MetadataReadPort.class);
        operator.graphReadPort = mock(com.metaforge.agent.cognition.api.port.GraphReadPort.class);
        operator.computeEngineReadPort = mock(com.metaforge.agent.cognition.api.port.ComputeEngineReadPort.class);
    }

    @Nested
    @DisplayName("executeWithPort 超时处理")
    class ExecuteWithPortTimeout {

        @Test
        @DisplayName("Port 调用异常时返回 CognitionResult.failure")
        void shouldReturnFailureOnPortException() {
            Supplier<Object> failingSupplier = () -> {
                throw new RuntimeException("OPERATOR_TIMEOUT: Port 调用超时");
            };

            Object result = operator.executeWithPort(failingSupplier);

            assertTrue(result instanceof CognitionResult);
            CognitionResult cr = (CognitionResult) result;
            assertFalse(cr.success());
            assertTrue(cr.error().contains("PORT_CALL_FAILED"));
        }

        @Test
        @DisplayName("非超时 Port 调用成功返回")
        void shouldReturnSuccessForNormalPortCall() {
            String expected = "normal-result";
            Supplier<String> normalSupplier = () -> expected;

            Object result = operator.executeWithPort(normalSupplier);

            assertEquals(expected, result);
        }

        @Test
        @DisplayName("最终 execute 返回失败标注")
        void shouldReturnFailureInExecute() {
            MetadataReadPort failingPort = mock(MetadataReadPort.class);
            when(failingPort.getByFqn("test")).thenThrow(new RuntimeException("OPERATOR_TIMEOUT"));
            operator.metadataReadPort = failingPort;

            CognitionQueryContext ctx = new CognitionQueryContext(
                    "TEST", "test.timed", DimensionCategory.ONTOLOGICAL,
                    null, null, "test", null, null, null, null, 20,
                    java.util.Collections.emptyMap(), java.util.Collections.emptyMap()
            );

            CognitionResult result = operator.execute(ctx);

            assertFalse(result.success());
            assertEquals("test.timed", result.operatorId());
            assertTrue(result.error().contains("PORT_CALL_FAILED"));
        }
    }
}
