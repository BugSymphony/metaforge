package com.metaforge.agent.cognition.core.domain.service;

import com.metaforge.agent.cognition.api.spi.CognitionOperator;
import com.metaforge.agent.cognition.api.spi.CognitionQueryContext;
import com.metaforge.agent.cognition.api.spi.CognitionResult;
import com.metaforge.agent.cognition.core.domain.model.aggregate.CognitionQuery;
import com.metaforge.agent.cognition.core.domain.model.entity.OperatorDefinition;
import com.metaforge.agent.cognition.core.domain.model.entity.TemplateDefinition;
import com.metaforge.agent.cognition.core.domain.model.valueobject.OperatorId;
import com.metaforge.agent.cognition.core.infrastructure.config.CognitionConfigProperties;
import com.metaforge.agent.cognition.core.infrastructure.registry.OperatorRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;

@Service
public class OperatorOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(OperatorOrchestrationService.class);

    private final OperatorRegistry operatorRegistry;
    private final CognitionConfigProperties configProperties;

    private final ExecutorService operatorExecutor;

    public OperatorOrchestrationService(OperatorRegistry operatorRegistry,
                                         @Autowired(required = false) CognitionConfigProperties configProperties) {
        this.operatorRegistry = operatorRegistry;
        this.configProperties = configProperties;
        this.operatorExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @PreDestroy
    public void shutdown() {
        operatorExecutor.shutdownNow();
    }

    public void orchestrate(CognitionQuery query) {
        List<OperatorDefinition> operators = query.getOperators();
        if (operators == null || operators.isEmpty()) {
            log.info("无算子待编排执行");
            return;
        }

        List<OperatorDefinition> sorted = operators.stream()
                .sorted(Comparator.comparingInt(OperatorDefinition::getPriority).reversed())
                .toList();

        List<Future<CognitionResult>> futures = new ArrayList<>(sorted.size());
        for (OperatorDefinition opDef : sorted) {
            futures.add(operatorExecutor.submit(() -> executeOperator(opDef, query)));
        }

        Map<OperatorId, CognitionResult> results = new LinkedHashMap<>();
        List<OperatorDefinition> failed = new ArrayList<>();
        int requiredFailureIndex = -1;
        OperatorDefinition requiredFailure = null;

        for (int i = 0; i < sorted.size(); i++) {
            OperatorDefinition opDef = sorted.get(i);
            Future<CognitionResult> future = futures.get(i);
            try {
                CognitionResult result = future.get(opDef.getTimeoutMs(), TimeUnit.MILLISECONDS);
                if (!result.success() && opDef.isRequired()) {
                    requiredFailureIndex = i;
                    requiredFailure = opDef;
                    break;
                }
                results.put(new OperatorId(opDef.getOperatorId()), result);
            } catch (TimeoutException e) {
                log.warn("算子执行超时: {} ({}ms)", opDef.getOperatorId(), opDef.getTimeoutMs());
                future.cancel(true);
                if (opDef.isRequired()) {
                    requiredFailureIndex = i;
                    requiredFailure = opDef;
                    break;
                }
                failed.add(opDef);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                requiredFailureIndex = i;
                requiredFailure = opDef;
                break;
            } catch (ExecutionException e) {
                if (e.getCause() instanceof com.metaforge.agent.cognition.api.exception.InvalidLevelException ile) {
                    // 业务异常穿透，交由异常处理器映射 INVALID_LEVEL(34013)
                    throw ile;
                }
                log.error("算子执行异常: {}", opDef.getOperatorId(), e.getCause());
                if (opDef.isRequired()) {
                    requiredFailureIndex = i;
                    requiredFailure = opDef;
                    break;
                }
                failed.add(opDef);
            }
        }

        if (requiredFailure != null) {
            cancelRemaining(futures, requiredFailureIndex);
            for (Map.Entry<OperatorId, CognitionResult> entry : results.entrySet()) {
                query.addExecutionResult(entry.getKey(), entry.getValue());
            }
            throw new RuntimeException("required 算子执行失败: " + requiredFailure.getOperatorId());
        }

        for (Map.Entry<OperatorId, CognitionResult> entry : results.entrySet()) {
            query.addExecutionResult(entry.getKey(), entry.getValue());
        }

        if (!failed.isEmpty()) {
            log.warn("{} 个非强制算子执行失败: {}", failed.size(),
                    failed.stream().map(OperatorDefinition::getOperatorId).toList());
        }
    }

    private void cancelRemaining(List<Future<CognitionResult>> futures, int fromIndex) {
        if (fromIndex < 0) {
            return;
        }
        for (int i = fromIndex; i < futures.size(); i++) {
            futures.get(i).cancel(true);
        }
    }

    private CognitionResult executeOperator(OperatorDefinition opDef, CognitionQuery query) {
        CognitionOperator operator = operatorRegistry.resolve(opDef.getOperatorId());
        if (operator == null) {
            log.warn("算子未注册，返回空结果: {}", opDef.getOperatorId());
            return CognitionResult.failure(opDef.getOperatorId(), null,
                    "算子未注册: " + opDef.getOperatorId());
        }

        CognitionQueryContext context = buildContext(opDef, query);

        try {
            return operator.execute(context);
        } catch (com.metaforge.agent.cognition.api.exception.InvalidLevelException e) {
            // 业务异常穿透，交由异常处理器映射 INVALID_LEVEL(34013)
            throw e;
        } catch (Exception e) {
            return CognitionResult.failure(opDef.getOperatorId(), operator.category(),
                    e.getMessage());
        }
    }

    private CognitionQueryContext buildContext(OperatorDefinition opDef, CognitionQuery query) {
        Map<String, Object> params = query.getRequest().params();
        TemplateDefinition def = query.getTemplateDefinition();
        Map<String, Object> templateConfig = def != null ? def.getGlobalConfig() : null;
        Map<String, Object> operatorConfig = def != null ? def.getOperatorConfig(opDef.getOperatorId()) : null;
        return new CognitionQueryContext(
                query.getTemplateId().value(),
                opDef.getOperatorId(),
                null,
                query.getScope(),
                query.getScope().bundles(),
                (String) params.get("entity_fqn"),
                params,
                query.getAgentArchetype(),
                query.getCognitionDepth(),
                resolveCursor(params),
                resolvePageSize(params),
                templateConfig,
                operatorConfig
        );
    }

    private Integer resolveCursor(Map<String, Object> params) {
        Object cursor = params.get("cursor");
        if (cursor == null) {
            return null;
        }
        if (cursor instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(cursor.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int resolvePageSize(Map<String, Object> params) {
        Object size = params.get("page_size");
        if (size instanceof Number n) {
            return n.intValue();
        }
        if (size instanceof String s && !s.isBlank()) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        if (configProperties != null && configProperties.getDefaults() != null) {
            return configProperties.getDefaults().getPageSize();
        }
        return 20;
    }
}
