package com.metaforge.agent.cognition.core.domain.service;

import com.metaforge.agent.cognition.api.dto.request.CognitionRequest;
import com.metaforge.agent.cognition.api.dto.request.Scope;
import com.metaforge.agent.cognition.api.enums.AgentArchetype;
import com.metaforge.agent.cognition.api.enums.CognitionDepth;
import com.metaforge.agent.cognition.api.enums.DimensionCategory;
import com.metaforge.agent.cognition.api.spi.CognitionOperator;
import com.metaforge.agent.cognition.api.spi.CognitionQueryContext;
import com.metaforge.agent.cognition.api.spi.CognitionResult;
import com.metaforge.agent.cognition.core.domain.model.aggregate.CognitionQuery;
import com.metaforge.agent.cognition.core.domain.model.entity.OperatorDefinition;
import com.metaforge.agent.cognition.core.domain.model.entity.TemplateDefinition;
import com.metaforge.agent.cognition.core.domain.model.valueobject.TemplateId;
import com.metaforge.agent.cognition.core.infrastructure.registry.OperatorRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("算子编排（真并行 + 虚拟线程）单元测试")
class OperatorOrchestrationServiceTest {

    OperatorRegistry operatorRegistry;
    OperatorOrchestrationService service;

    @BeforeEach
    void setUp() {
        operatorRegistry = mock(OperatorRegistry.class);
        service = new OperatorOrchestrationService(operatorRegistry, null);
    }

    private OperatorDefinition op(String id, int priority, boolean required, long timeoutMs) {
        OperatorDefinition d = new OperatorDefinition();
        d.setOperatorId(id);
        d.setPriority(priority);
        d.setRequired(required);
        d.setTimeoutMs(timeoutMs);
        return d;
    }

    private CognitionQuery query(List<OperatorDefinition> operators) {
        TemplateDefinition def = new TemplateDefinition();
        def.setTemplateId("TEST");
        def.setOperators(operators);
        CognitionRequest req = new CognitionRequest(
                Scope.EMPTY, Map.of(), "JSON", CognitionDepth.L3, AgentArchetype.EXECUTION, 8000);
        CognitionQuery q = new CognitionQuery(new TemplateId("TEST"), req);
        q.loadTemplate(def);
        return q;
    }

    private CognitionOperator successOp(String id, Object data) {
        return new CognitionOperator() {
            @Override
            public String operatorId() {
                return id;
            }

            @Override
            public DimensionCategory category() {
                return DimensionCategory.ONTOLOGICAL;
            }

            @Override
            public CognitionResult execute(CognitionQueryContext context) {
                return CognitionResult.success(id, category(), data);
            }
        };
    }

    private CognitionOperator failingOp(String id) {
        return new CognitionOperator() {
            @Override
            public String operatorId() {
                return id;
            }

            @Override
            public DimensionCategory category() {
                return DimensionCategory.ONTOLOGICAL;
            }

            @Override
            public CognitionResult execute(CognitionQueryContext context) {
                return CognitionResult.failure(id, category(), "模拟失败");
            }
        };
    }

    private CognitionOperator blockingOp(String id) {
        return new CognitionOperator() {
            @Override
            public String operatorId() {
                return id;
            }

            @Override
            public DimensionCategory category() {
                return DimensionCategory.ONTOLOGICAL;
            }

            @Override
            public CognitionResult execute(CognitionQueryContext context) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return CognitionResult.success(id, category(), "blocked-done");
            }
        };
    }

    @Test
    @DisplayName("按 priority 降序收集结果")
    void shouldCollectResultsByPriority() {
        when(operatorRegistry.resolve("op.low")).thenReturn(successOp("op.low", "low"));
        when(operatorRegistry.resolve("op.high")).thenReturn(successOp("op.high", "high"));

        List<OperatorDefinition> ops = List.of(
                op("op.low", 10, true, 1000),
                op("op.high", 100, true, 1000));

        CognitionQuery q = query(ops);
        service.orchestrate(q);

        assertThat(q.getExecutionResults().keySet())
                .extracting(k -> k.value())
                .containsExactly("op.high", "op.low");
    }

    @Test
    @DisplayName("多个算子真实并行执行")
    void shouldExecuteInParallel() throws Exception {
        when(operatorRegistry.resolve("op.slow")).thenReturn(new CognitionOperator() {
            @Override
            public String operatorId() {
                return "op.slow";
            }

            @Override
            public DimensionCategory category() {
                return DimensionCategory.ONTOLOGICAL;
            }

            @Override
            public CognitionResult execute(CognitionQueryContext context) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return CognitionResult.success("op.slow", category(), "slow");
            }
        });
        when(operatorRegistry.resolve("op.fast")).thenReturn(new CognitionOperator() {
            @Override
            public String operatorId() {
                return "op.fast";
            }

            @Override
            public DimensionCategory category() {
                return DimensionCategory.ONTOLOGICAL;
            }

            @Override
            public CognitionResult execute(CognitionQueryContext context) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return CognitionResult.success("op.fast", category(), "fast");
            }
        });

        List<OperatorDefinition> ops = List.of(
                op("op.slow", 100, true, 2000),
                op("op.fast", 90, true, 2000));

        CognitionQuery q = query(ops);
        long start = System.nanoTime();
        service.orchestrate(q);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        // 真并行：两个 300ms 算子应在 ~300ms 内完成（串行则 ~600ms）
        assertThat(elapsedMs).isLessThan(450);
        assertThat(q.getExecutionResults()).hasSize(2);
    }

    @Test
    @DisplayName("算子超时后取消任务并失败")
    void shouldCancelOnTimeout() {
        when(operatorRegistry.resolve("op.blocking")).thenReturn(blockingOp("op.blocking"));

        List<OperatorDefinition> ops = List.of(
                op("op.blocking", 100, false, 100));

        CognitionQuery q = query(ops);

        // 非强制算子超时 → 跳过不抛异常
        service.orchestrate(q);
        assertThat(q.getExecutionResults()).isEmpty();
    }

    @Test
    @DisplayName("required 算子超时 → 整体失败")
    void shouldFailTemplateWhenRequiredTimeout() {
        when(operatorRegistry.resolve("op.blocking")).thenReturn(blockingOp("op.blocking"));

        List<OperatorDefinition> ops = List.of(
                op("op.blocking", 100, true, 100));

        CognitionQuery q = query(ops);

        assertThatThrownBy(() -> service.orchestrate(q))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("required 算子执行失败");
    }

    @Test
    @DisplayName("required 算子返回 failure → 整体失败")
    void shouldFailTemplateWhenRequiredReturnsFailure() {
        when(operatorRegistry.resolve("op.fail")).thenReturn(failingOp("op.fail"));

        List<OperatorDefinition> ops = List.of(
                op("op.fail", 100, true, 1000));

        CognitionQuery q = query(ops);

        assertThatThrownBy(() -> service.orchestrate(q))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("required 算子执行失败");
    }

    @Test
    @DisplayName("required=false 算子返回 failure → 记录失败结果但不中断")
    void shouldSkipOptionalFailure() {
        when(operatorRegistry.resolve("op.fail")).thenReturn(failingOp("op.fail"));
        when(operatorRegistry.resolve("op.ok")).thenReturn(successOp("op.ok", "ok"));

        List<OperatorDefinition> ops = List.of(
                op("op.fail", 100, false, 1000),
                op("op.ok", 90, true, 1000));

        CognitionQuery q = query(ops);
        service.orchestrate(q);

        // 不抛异常，两条结果均记录（部分成功语义），required 成功算子保留
        assertThat(q.getExecutionResults()).hasSize(2);
        assertThat(q.getExecutionResults().values())
                .filteredOn(CognitionResult::success)
                .extracting(CognitionResult::operatorId)
                .containsExactly("op.ok");
    }

    @Test
    @DisplayName("无算子时编排空转")
    void shouldNoopWhenNoOperators() {
        CognitionQuery q = query(List.of());
        service.orchestrate(q);
        assertThat(q.getExecutionResults()).isEmpty();
    }

    @Test
    @DisplayName("operators 缺省时保留全部算子")
    void shouldKeepAllOperatorsWhenOperatorsAbsent() {
        List<OperatorDefinition> ops = List.of(
                op("op.a", 100, true, 1000),
                op("op.b", 90, true, 1000));
        CognitionQuery q = query(ops);

        q.filterByOperators();

        assertThat(q.getOperators()).hasSize(2);
    }

    @Test
    @DisplayName("operators 指定子集时仅保留匹配算子")
    void shouldFilterToSelectedOperators() {
        List<OperatorDefinition> ops = List.of(
                op("op.a", 100, true, 1000),
                op("op.b", 90, true, 1000));
        TemplateDefinition def = new TemplateDefinition();
        def.setTemplateId("TEST");
        def.setOperators(ops);
        CognitionRequest req = new CognitionRequest(
                Scope.EMPTY, Map.of("selectOperators", List.of("op.b")), "JSON",
                CognitionDepth.L3, AgentArchetype.EXECUTION, 8000);
        CognitionQuery q = new CognitionQuery(new TemplateId("TEST"), req);
        q.loadTemplate(def);

        q.filterByOperators();

        assertThat(q.getOperators())
                .extracting(OperatorDefinition::getOperatorId)
                .containsExactly("op.b");
    }

    @Test
    @DisplayName("operators 全不匹配时抛 InvalidOperatorSelectionException(34014)")
    void shouldFailWhenNoOperatorMatchesSelection() {
        List<OperatorDefinition> ops = List.of(
                op("op.a", 100, true, 1000));
        TemplateDefinition def = new TemplateDefinition();
        def.setTemplateId("TEST");
        def.setOperators(ops);
        CognitionRequest req = new CognitionRequest(
                Scope.EMPTY, Map.of("selectOperators", List.of("op.unknown")), "JSON",
                CognitionDepth.L3, AgentArchetype.EXECUTION, 8000);
        CognitionQuery q = new CognitionQuery(new TemplateId("TEST"), req);
        q.loadTemplate(def);

        assertThatThrownBy(q::filterByOperators)
                .isInstanceOf(com.metaforge.agent.cognition.api.exception.InvalidOperatorSelectionException.class);
    }
}
