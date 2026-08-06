package com.metaforge.agent.cognition.core.application.service;

import com.metaforge.agent.cognition.api.dto.request.CognitionRequest;
import com.metaforge.agent.cognition.api.dto.response.GuidanceResult;
import com.metaforge.agent.cognition.api.enums.AgentArchetype;
import com.metaforge.agent.cognition.api.enums.CognitionDepth;
import com.metaforge.agent.cognition.api.enums.ContextMode;
import com.metaforge.agent.cognition.api.enums.OutputFormat;
import com.metaforge.agent.cognition.api.enums.PerspectiveCode;
import com.metaforge.agent.cognition.api.enums.ScopeMode;
import com.metaforge.agent.cognition.api.service.CognitionQueryService;
import com.metaforge.agent.cognition.core.domain.exception.EmptyBundleFqnsException;
import com.metaforge.agent.cognition.core.domain.exception.InvalidEntityFqnException;
import com.metaforge.agent.cognition.core.domain.exception.TemplateNotFoundException;
import com.metaforge.agent.cognition.core.domain.service.FqnValidationService;
import com.metaforge.agent.cognition.core.domain.service.PerspectiveOrchestrationService;
import com.metaforge.agent.cognition.core.domain.service.ScopeNarrowingService;
import com.metaforge.agent.cognition.core.domain.service.ScopeResolutionServiceImpl;
import com.metaforge.agent.cognition.core.domain.service.TemplateResolutionService;
import com.metaforge.agent.cognition.core.domain.service.VersionAnchorService;
import com.metaforge.agent.cognition.core.domain.model.valueobject.DataVersionAnchor;
import com.metaforge.agent.cognition.core.infrastructure.config.TemplateConfig;
import com.metaforge.agent.cognition.core.infrastructure.config.AgentCognitionProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CognitionQueryServiceImpl implements CognitionQueryService {

    private static final Logger log = LoggerFactory.getLogger(CognitionQueryServiceImpl.class);

    private final TemplateConfig templateConfig;
    private final PerspectiveOrchestrationService orchestrationService;
    private final AgentCognitionProperties properties;
    private final FqnValidationService fqnValidationService;
    private final ScopeResolutionServiceImpl scopeResolutionService;
    private final VersionAnchorService versionAnchorService;
    private final ScopeNarrowingService scopeNarrowingService;

    public CognitionQueryServiceImpl(TemplateConfig templateConfig,
                                      PerspectiveOrchestrationService orchestrationService,
                                      AgentCognitionProperties properties,
                                      FqnValidationService fqnValidationService,
                                      ScopeResolutionServiceImpl scopeResolutionService,
                                      VersionAnchorService versionAnchorService,
                                      ScopeNarrowingService scopeNarrowingService) {
        this.templateConfig = templateConfig;
        this.orchestrationService = orchestrationService;
        this.properties = properties;
        this.fqnValidationService = fqnValidationService;
        this.scopeResolutionService = scopeResolutionService;
        this.versionAnchorService = versionAnchorService;
        this.scopeNarrowingService = scopeNarrowingService;
    }

    @Override
    public GuidanceResult execute(String templateId, CognitionRequest request) {
        log.info("执行认知查询: templateId={}, bundleFqns={}, entityFqn={}",
                templateId, request.bundleFqns(), request.entityFqn());

        List<String> bundleFqns = resolveBundleFqns(request);

        List<DataVersionAnchor> versionAnchors = versionAnchorService.resolveAnchors(bundleFqns);

        ContextMode contextMode = deriveContextMode(request);
        String depthStr = request.cognitionDepth() != null ? request.cognitionDepth() : properties.getDefaultDepth();
        CognitionDepth depth = CognitionDepth.fromString(depthStr);

        String archetypeStr = request.agentArchetype() != null ? request.agentArchetype() : properties.getDefaultArchetype();
        AgentArchetype archetype = AgentArchetype.fromString(archetypeStr);

        int maxTokens = request.maxTokens() != null ? request.maxTokens() : properties.getDefaultMaxTokens();

        List<PerspectiveCode> perspectiveCodes =
                buildPerspectiveList(templateId, request, depth);

        ScopeMode scopeMode = request.scopeMode() != null ? ScopeMode.fromString(request.scopeMode()) : ScopeMode.INHERITED;
        List<String> narrowedEntityFqns = null;
        List<String> narrowedSchemaFqns = null;

        if (scopeMode == ScopeMode.PURE) {
            perspectiveCodes = List.of(PerspectiveCode.ENTITY_PROFILE);
            log.debug("PURE 模式: 仅使用 entity_profile 视角");
        } else if (scopeMode == ScopeMode.INHERITED && request.entityFqn() != null) {
            var narrowedScope = scopeNarrowingService.narrow(request.entityFqn());
            narrowedEntityFqns = narrowedScope.relatedEntityFqns();
            narrowedSchemaFqns = narrowedScope.relatedSchemaFqns();
            log.info("INHERITED 模式: 三层收窄完成 - blueprintSteps={}, relatedEntities={}, relatedSchemas={}",
                    narrowedScope.blueprintStepFqns().size(),
                    narrowedScope.relatedEntityFqns().size(),
                    narrowedScope.relatedSchemaFqns().size());
        }

        TemplateResolutionService.ExecutionPlan executionPlan = new TemplateResolutionService.ExecutionPlan(
                perspectiveCodes, depth, archetype, maxTokens,
                OutputFormat.JSON, contextMode, scopeMode);

        PerspectiveOrchestrationService.OrchestrationContext orchestrationContext =
                new PerspectiveOrchestrationService.OrchestrationContext(
                        bundleFqns, request.entityFqn(), request.entityTypes(),
                        request.subjectDomainFqn(), request.contextParameters(),
                        request.cursor(), request.pageSize(), request.expand(),
                        narrowedEntityFqns, narrowedSchemaFqns);

        com.metaforge.agent.cognition.core.domain.model.aggregate.GuidanceResult domainResult =
                orchestrationService.orchestrate(executionPlan, orchestrationContext);

        domainResult.getContextMeta().setDataVersionAnchors(versionAnchors);
        domainResult.getContextMeta().setScopeMode(scopeMode);
        domainResult.getContextMeta().setCognitionDepth(depth);
        domainResult.getContextMeta().setAgentArchetype(archetype);

        if (contextMode == ContextMode.ENTITY_LEVEL && request.entityFqn() != null) {
            var adjacentContext = scopeResolutionService.buildAdjacentContext(request.entityFqn());
            domainResult.getContextMeta().setAdjacentContext(adjacentContext);
        }

        log.info("认知查询执行完成: templateId={}, appliedPerspectives={}, contextMode={}",
                templateId,
                domainResult.getContextMeta().getAppliedPerspectives() != null
                        ? domainResult.getContextMeta().getAppliedPerspectives().size() : 0,
                contextMode);

        return mapToApiResult(templateId, domainResult);
    }

    private ContextMode deriveContextMode(CognitionRequest request) {
        return request.entityFqn() != null && !request.entityFqn().isBlank()
                ? ContextMode.ENTITY_LEVEL : ContextMode.BUNDLE_LEVEL;
    }

    private List<String> resolveBundleFqns(CognitionRequest request) {
        List<String> bundleFqns = request.bundleFqns();
        String entityFqn = request.entityFqn();

        if ((bundleFqns == null || bundleFqns.isEmpty()) && entityFqn != null && !entityFqn.isBlank()) {
            String resolvedBundle = fqnValidationService.resolveBundleFromEntityFqn(entityFqn);
            bundleFqns = List.of(resolvedBundle);
            log.info("从 entity_fqn 自动推导 bundle_fqns: entityFqn={}, bundleFqns={}", entityFqn, bundleFqns);
        }

        if (bundleFqns == null || bundleFqns.isEmpty()) {
            throw new EmptyBundleFqnsException(
                    "bundle_fqns 不能为空，且未提供 entity_fqn 用于自动推导 Bundle 范围");
        }

        fqnValidationService.validateBundleFqns(bundleFqns);

        List<String> normalized = bundleFqns.stream()
                .map(fqn -> {
                    int colon = fqn.indexOf(':');
                    return colon > 0 ? fqn.substring(0, colon) : fqn;
                })
                .distinct()
                .toList();
        log.debug("归一化 Bundle FQN: {} -> {}", bundleFqns, normalized);
        return normalized;
    }

    private List<PerspectiveCode> buildPerspectiveList(
            String templateId, CognitionRequest request, CognitionDepth depth) {

        List<String> perspectiveIds;
        List<String> requested = request.perspectives();
        if (requested != null && !requested.isEmpty()) {
            perspectiveIds = requested;
        } else {
            TemplateConfig.TemplateDefinition templateDef = templateConfig.getTemplate(templateId);
            if (templateDef == null) {
                throw new TemplateNotFoundException("模板未注册: " + templateId);
            }
            perspectiveIds = templateDef.getPerspectives();
            if (perspectiveIds == null || perspectiveIds.isEmpty()) {
                throw new TemplateNotFoundException("模板未配置任何视角: " + templateId);
            }
        }

        return perspectiveIds.stream()
                .map(id -> {
                    try {
                        return PerspectiveCode.fromString(id);
                    } catch (IllegalArgumentException e) {
                        log.warn("忽略未知视角标识: {}", id);
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    private GuidanceResult mapToApiResult(String templateId,
                                           com.metaforge.agent.cognition.core.domain.model.aggregate.GuidanceResult domainResult) {
        GuidanceResult apiResult = new GuidanceResult();
        apiResult.setTemplateId(templateId);

        com.metaforge.agent.cognition.api.dto.response.ContextMeta apiContextMeta =
                new com.metaforge.agent.cognition.api.dto.response.ContextMeta();
        var domainMeta = domainResult.getContextMeta();

        if (domainMeta != null) {
            apiContextMeta.setBundleFqns(domainMeta.getBundleFqns());
            apiContextMeta.setEntityFqn(domainMeta.getEntityFqn());
            apiContextMeta.setContextMode(domainMeta.getContextMode());
            apiContextMeta.setScopeMode(domainMeta.getScopeMode());
            apiContextMeta.setCognitionDepth(domainMeta.getCognitionDepth());
            apiContextMeta.setAgentArchetype(domainMeta.getAgentArchetype());
            apiContextMeta.setAppliedPerspectives(domainMeta.getAppliedPerspectives());
            apiContextMeta.setSkippedPerspectives(domainMeta.getSkippedPerspectives());
            apiContextMeta.setSkipReasons(domainMeta.getSkipReasons());
            apiContextMeta.setTotalTokenCount(domainMeta.getTotalTokenCount());
            apiContextMeta.setTokenTrimmed(domainMeta.isTokenTrimmed());
            apiContextMeta.setTruncated(domainMeta.isTruncated());
            apiContextMeta.setQueriedAt(domainMeta.getQueriedAt());

            if (domainMeta.getTruncations() != null) {
                apiContextMeta.setTruncations(
                        domainMeta.getTruncations().stream()
                                .map(t -> new com.metaforge.agent.cognition.api.dto.response.ContextMeta.TruncationNote(
                                        PerspectiveCode.fromString(t.perspective().value()),
                                        t.reason()))
                                .toList());
            }

            if (domainMeta.getDataVersionAnchors() != null) {
                apiContextMeta.setDataVersionAnchors(
                        domainMeta.getDataVersionAnchors().stream()
                                .map(a -> new com.metaforge.agent.cognition.api.dto.DataVersionAnchor(
                                        a.bundleFqn(), a.publishedVersionFqn(),
                                        a.latestVersionNumber(), a.queriedAt()))
                                .toList());
            }

            if (domainMeta.getAdjacentContext() != null) {
                apiContextMeta.setAdjacentContext(
                        new com.metaforge.agent.cognition.api.dto.response.AdjacentContext(
                                domainMeta.getAdjacentContext().previousSteps(),
                                domainMeta.getAdjacentContext().nextSteps(),
                                domainMeta.getAdjacentContext().upstreamEntities(),
                                domainMeta.getAdjacentContext().downstreamEntities()));
            }
        }

        apiResult.setContextMeta(apiContextMeta);
        apiResult.setPerspectives(domainResult.getPerspectiveChapters());

        return apiResult;
    }
}
