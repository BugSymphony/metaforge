package com.metaforge.agent.cognition.core.application.service;

import com.metaforge.agent.cognition.api.dto.request.CognitionRequest;
import com.metaforge.agent.cognition.api.dto.response.CognitionResponse;
import com.metaforge.agent.cognition.api.dto.response.ContextMeta;
import com.metaforge.agent.cognition.api.service.CognitionQueryService;
import com.metaforge.agent.cognition.core.domain.model.aggregate.CognitionQuery;
import com.metaforge.agent.cognition.core.domain.model.entity.TemplateDefinition;
import com.metaforge.agent.cognition.core.domain.model.valueobject.TemplateId;
import com.metaforge.agent.cognition.core.domain.exception.TemplateNotFoundException;
import com.metaforge.agent.cognition.core.domain.service.DepthTrimmingService;
import com.metaforge.agent.cognition.core.domain.service.OperatorOrchestrationService;
import com.metaforge.agent.cognition.core.domain.service.OutputAssemblyService;
import com.metaforge.agent.cognition.core.domain.service.ScopeResolutionService;
import com.metaforge.agent.cognition.core.domain.service.ScopeResolutionService.ScopeValidationResult;
import com.metaforge.agent.cognition.core.domain.service.TemplateResolutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;

@Service
public class CognitionQueryServiceImpl implements CognitionQueryService {

    private static final Logger log = LoggerFactory.getLogger(CognitionQueryServiceImpl.class);

    private final TemplateResolutionService templateResolutionService;
    private final ScopeResolutionService scopeResolutionService;
    private final DepthTrimmingService depthTrimmingService;
    private final OperatorOrchestrationService operatorOrchestrationService;
    private final OutputAssemblyService outputAssemblyService;

    @Autowired
    public CognitionQueryServiceImpl(TemplateResolutionService templateResolutionService,
                                      ScopeResolutionService scopeResolutionService,
                                      @Autowired(required = false) DepthTrimmingService depthTrimmingService,
                                      @Autowired(required = false) OperatorOrchestrationService operatorOrchestrationService,
                                      @Autowired(required = false) OutputAssemblyService outputAssemblyService) {
        this.templateResolutionService = templateResolutionService;
        this.scopeResolutionService = scopeResolutionService;
        this.depthTrimmingService = depthTrimmingService;
        this.operatorOrchestrationService = operatorOrchestrationService;
        this.outputAssemblyService = outputAssemblyService;
    }

    @Override
    public CognitionResponse execute(String templateIdStr, CognitionRequest request) {
        log.info("认知查询执行: templateId={}, format={}, depth={}, archetype={}",
                templateIdStr, request.format(), request.cognitionDepth(), request.agentArchetype());

        TemplateId templateId;
        try {
            templateId = new TemplateId(templateIdStr);
        } catch (IllegalArgumentException e) {
            log.warn("模板 ID 格式无效: {}", templateIdStr);
            throw new TemplateNotFoundException(templateIdStr);
        }

        TemplateDefinition definition = templateResolutionService.resolve(templateIdStr);
        if (definition == null) {
            log.warn("模板未注册: {}", templateIdStr);
            throw new TemplateNotFoundException(templateIdStr);
        }

        CognitionQuery query = new CognitionQuery(templateId, request);
        query.loadTemplate(definition);

        try {
            ScopeValidationResult validationResult = scopeResolutionService.validateScope(
                    request.scope(),
                    definition.getScopeBehavior() != null && definition.getScopeBehavior().isScopeRequired(),
                    (String) request.params().get("entity_fqn"));
            for (String skipped : validationResult.skippedEntities) {
                query.addSkippedEntity(skipped);
            }
        } catch (Exception e) {
            log.warn("Scope 校验失败: {}", e.getMessage());
            throw e;
        }

        query.filterByOperators();

        query.filterByArchetype();

        if (depthTrimmingService != null) {
            DepthTrimmingService.TrimResult trimResult =
                    depthTrimmingService.trim(query.getOperators(), query.getCognitionDepth());
            query.setOperators(trimResult.trimmedOperators);
            for (String truncated : trimResult.truncatedPerspectives) {
                query.addTruncatedPerspective(truncated);
            }
        }

        if (operatorOrchestrationService != null) {
            operatorOrchestrationService.orchestrate(query);
        } else {
            log.info("P1 MVP 空算子编排: OperatorOrchestrationService Bean 未注册");
        }

        if (outputAssemblyService != null) {
            return outputAssemblyService.assemble(query);
        }

        return buildFallbackResponse(templateIdStr, request);
    }

    private CognitionResponse buildFallbackResponse(String templateId, CognitionRequest request) {
        ContextMeta contextMeta = new ContextMeta(
                templateId,
                Collections.emptyList(),
                request.scope() != null ? request.scope() : com.metaforge.agent.cognition.api.dto.request.Scope.EMPTY,
                0,
                Instant.now(),
                Collections.emptyList(),
                Collections.emptyList()
        );
        return CognitionResponse.json(templateId, contextMeta, Collections.emptyList(), Collections.emptyMap());
    }
}
