package com.metaforge.agent.cognition.core.domain.service;

import com.metaforge.agent.cognition.api.enums.ContextMode;
import com.metaforge.agent.cognition.api.enums.PerspectiveCode;
import com.metaforge.agent.cognition.api.enums.PerspectiveScope;
import com.metaforge.agent.cognition.api.perspective.PerspectiveExecutionContext;
import com.metaforge.agent.cognition.api.perspective.PerspectiveExecutor;
import com.metaforge.agent.cognition.core.application.assembler.OutputAssembler;
import com.metaforge.agent.cognition.core.application.executor.PerspectiveRegistry;
import com.metaforge.agent.cognition.core.domain.model.aggregate.GuidanceResult;
import com.metaforge.agent.cognition.core.domain.model.entity.PerspectiveResult;
import com.metaforge.agent.cognition.core.domain.model.valueobject.ContextMeta;
import com.metaforge.agent.cognition.core.infrastructure.config.PerspectiveConfig;
import com.metaforge.agent.cognition.core.infrastructure.config.AgentCognitionProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class PerspectiveOrchestrationServiceImpl implements PerspectiveOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(PerspectiveOrchestrationServiceImpl.class);

    private final PerspectiveRegistry perspectiveRegistry;
    private final PerspectiveConfig perspectiveConfig;
    private final AgentCognitionProperties properties;
    private final OutputAssembler outputAssembler;

    public PerspectiveOrchestrationServiceImpl(PerspectiveRegistry perspectiveRegistry,
                                                PerspectiveConfig perspectiveConfig,
                                                AgentCognitionProperties properties,
                                                OutputAssembler outputAssembler) {
        this.perspectiveRegistry = perspectiveRegistry;
        this.perspectiveConfig = perspectiveConfig;
        this.properties = properties;
        this.outputAssembler = outputAssembler;
    }

    @Override
    public GuidanceResult orchestrate(TemplateResolutionService.ExecutionPlan executionPlan,
                                       OrchestrationContext context) {
        List<PerspectiveCode> requestedPerspectives = executionPlan.perspectives();
        ContextMode contextMode = executionPlan.contextMode();

        ContextMeta contextMeta = new ContextMeta();
        contextMeta.setContextMode(contextMode);
        contextMeta.setBundleFqns(context.bundleFqns());
        contextMeta.setEntityFqn(context.entityFqn());

        List<PerspectiveResult> results = new ArrayList<>();
        List<String> appliedPerspectives = new ArrayList<>();
        List<String> skippedPerspectives = new ArrayList<>();
        List<String> skipReasons = new ArrayList<>();

        for (PerspectiveCode perspectiveCode : requestedPerspectives) {
            String perspectiveId = perspectiveCode.getValue();
            long startTime = System.currentTimeMillis();

            PerspectiveConfig.PerspectiveDefinition def = perspectiveConfig.getPerspectives().stream()
                    .filter(p -> p.getPerspectiveId().equals(perspectiveId))
                    .findFirst().orElse(null);

            if (def == null) {
                skippedPerspectives.add(perspectiveId);
                skipReasons.add("Perspective not found in config: " + perspectiveId);
                continue;
            }

            if (!isScopeCompatible(def.getScope(), contextMode)) {
                skippedPerspectives.add(perspectiveId);
                skipReasons.add(String.format("%s-scope perspective skipped in %s context",
                        def.getScope(), contextMode));
                continue;
            }

            PerspectiveExecutor executor = perspectiveRegistry.getExecutor(perspectiveId);
            if (executor == null) {
                skippedPerspectives.add(perspectiveId);
                skipReasons.add("No executor registered for: " + perspectiveId);
                continue;
            }

            int depthCap = executionPlan.depth() != null ? executionPlan.depth().maxPerspectives() : 7;
            if (appliedPerspectives.size() >= depthCap) {
                skippedPerspectives.add(perspectiveId);
                skipReasons.add("深度上限 " + depthCap + " 个视角已满，跳过: " + perspectiveId);
                continue;
            }

            try {
                PerspectiveExecutionContext execCtx = buildExecutionContext(contextMode, context);
                PerspectiveResult result = executeWithTimeout(executor, execCtx, perspectiveId);
                results.add(result);
                appliedPerspectives.add(perspectiveId);
                log.info("视角执行完成: perspectiveId={}, duration={}ms, truncated={}",
                        perspectiveId, result.getExecutionDurationMs(), result.isTruncated());
            } catch (Exception e) {
                log.warn("视角执行异常: {}, error={}", perspectiveId, e.getMessage());
                PerspectiveResult errorResult = new PerspectiveResult();
                errorResult.setPerspectiveCode(new com.metaforge.agent.cognition.core.domain.model.valueobject.PerspectiveCode(perspectiveId));
                errorResult.setTruncated(true);
                errorResult.setTruncatedReason("ERROR");
                errorResult.setExecutionDurationMs(System.currentTimeMillis() - startTime);
                results.add(errorResult);
                appliedPerspectives.add(perspectiveId);
            }
        }

        contextMeta.setAppliedPerspectives(appliedPerspectives);
        contextMeta.setSkippedPerspectives(skippedPerspectives);
        contextMeta.setSkipReasons(skipReasons);

        return outputAssembler.assemble(
                results, contextMeta,
                executionPlan.depth(), executionPlan.archetype(),
                executionPlan.maxTokens());
    }

    private boolean isScopeCompatible(PerspectiveScope scope, ContextMode contextMode) {
        if (contextMode == ContextMode.BUNDLE_LEVEL) {
            return scope == PerspectiveScope.BUNDLE || scope == PerspectiveScope.BOTH;
        } else {
            return scope == PerspectiveScope.ENTITY || scope == PerspectiveScope.BOTH;
        }
    }

    private PerspectiveExecutionContext buildExecutionContext(ContextMode contextMode,
                                                               OrchestrationContext context) {
        return new PerspectiveExecutionContext(
                contextMode,
                context.bundleFqns(),
                context.entityFqn(),
                context.entityTypes(),
                context.subjectDomainFqn(),
                context.contextParameters(),
                context.cursor(),
                context.pageSize(),
                context.expand(),
                context.narrowedEntityFqns(),
                context.narrowedSchemaFqns());
    }

    private PerspectiveResult executeWithTimeout(PerspectiveExecutor executor,
                                                  PerspectiveExecutionContext ctx,
                                                  String perspectiveId) {
        long startTime = System.currentTimeMillis();
        CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> executor.execute(ctx));

        try {
            Object data = future.orTimeout(properties.getPerspectiveTimeoutMs(), TimeUnit.MILLISECONDS).get();

            PerspectiveResult result = new PerspectiveResult();
            result.setPerspectiveCode(new com.metaforge.agent.cognition.core.domain.model.valueobject.PerspectiveCode(perspectiveId));
            result.setData(data);
            result.setTruncated(false);
            result.setExecutionDurationMs(System.currentTimeMillis() - startTime);
            return result;
        } catch (ExecutionException e) {
            future.cancel(true);
            Throwable cause = e.getCause();
            if (cause instanceof TimeoutException) {
                log.warn("视角执行超时: perspectiveId={}, timeoutMs={}",
                        perspectiveId, properties.getPerspectiveTimeoutMs());
                return buildTimeoutResult(perspectiveId, startTime);
            }
            throw new RuntimeException("视角执行失败: " + perspectiveId, cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            return buildTimeoutResult(perspectiveId, startTime);
        }
    }

    private PerspectiveResult buildTimeoutResult(String perspectiveId, long startTime) {
        PerspectiveResult result = new PerspectiveResult();
        result.setPerspectiveCode(new com.metaforge.agent.cognition.core.domain.model.valueobject.PerspectiveCode(perspectiveId));
        result.setTruncated(true);
        result.setTruncatedReason("TIMEOUT");
        result.setExecutionDurationMs(System.currentTimeMillis() - startTime);
        return result;
    }
}
