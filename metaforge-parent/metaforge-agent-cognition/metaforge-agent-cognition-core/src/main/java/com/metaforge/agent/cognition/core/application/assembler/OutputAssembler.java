package com.metaforge.agent.cognition.core.application.assembler;

import com.metaforge.agent.cognition.api.enums.AgentArchetype;
import com.metaforge.agent.cognition.api.enums.CognitionDepth;
import com.metaforge.agent.cognition.core.domain.model.aggregate.GuidanceResult;
import com.metaforge.agent.cognition.core.domain.model.entity.PerspectiveResult;
import com.metaforge.agent.cognition.core.domain.model.valueobject.ContextMeta;
import com.metaforge.agent.cognition.core.domain.service.ArchetypeOrderingService;
import com.metaforge.agent.cognition.core.domain.service.DepthTrimmingService;
import com.metaforge.agent.cognition.core.domain.service.TokenBudgetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OutputAssembler {

    private static final Logger log = LoggerFactory.getLogger(OutputAssembler.class);

    private final DepthTrimmingService depthTrimmingService;
    private final ArchetypeOrderingService archetypeOrderingService;
    private final TokenBudgetService tokenBudgetService;

    public OutputAssembler(DepthTrimmingService depthTrimmingService,
                           ArchetypeOrderingService archetypeOrderingService,
                           TokenBudgetService tokenBudgetService) {
        this.depthTrimmingService = depthTrimmingService;
        this.archetypeOrderingService = archetypeOrderingService;
        this.tokenBudgetService = tokenBudgetService;
    }

    /**
     * 组装视角结果为统一输出聚合根。
     * <p>处理顺序：先按 agent 原型对视角章节排序，再按认知深度裁剪，
     * 最后按 maxTokens 执行 Token 预算裁剪。</p>
     */
    public GuidanceResult assemble(List<PerspectiveResult> results,
                                    ContextMeta contextMeta,
                                    CognitionDepth depth,
                                    AgentArchetype archetype,
                                    int maxTokens) {
        GuidanceResult guidanceResult = GuidanceResult.create(contextMeta);

        for (PerspectiveResult result : results) {
            if (result.getData() != null) {
                guidanceResult.putPerspectiveChapter(result.getPerspectiveCode().value(), result.getData());
            }
        }

        contextMeta.setQueriedAt(Instant.now());

        if (contextMeta.getDataVersionAnchors() == null) {
            contextMeta.setDataVersionAnchors(new ArrayList<>());
        }
        if (contextMeta.getTruncations() == null) {
            contextMeta.setTruncations(new ArrayList<>());
        }

        applyArchetypeOrdering(guidanceResult, archetype);
        applyDepthTrim(guidanceResult, depth);
        collectTruncationInfo(guidanceResult, results);

        if (maxTokens > 0) {
            tokenBudgetService.trim(guidanceResult, maxTokens);
        }

        contextMeta.setTotalTokenCount(tokenBudgetService.estimateTokens(guidanceResult));

        log.debug("输出组装完成: archetype={}, depth={}, chapters={}",
                archetype, depth,
                guidanceResult.getPerspectiveChapters() != null ? guidanceResult.getPerspectiveChapters().size() : 0);

        return guidanceResult;
    }

    private void applyArchetypeOrdering(GuidanceResult result, AgentArchetype archetype) {
        Map<String, Object> chapters = result.getPerspectiveChapters();
        if (chapters == null || chapters.isEmpty()) {
            return;
        }
        List<String> orderedIds = archetypeOrderingService.orderPerspectives(new ArrayList<>(chapters.keySet()), archetype);
        Map<String, Object> orderedChapters = new LinkedHashMap<>();
        for (String id : orderedIds) {
            orderedChapters.put(id, chapters.get(id));
        }
        result.setPerspectiveChapters(orderedChapters);
    }

    private void applyDepthTrim(GuidanceResult result, CognitionDepth depth) {
        if (depth == null) return;
        int maxPerspectives = depthTrimmingService.getMaxPerspectives(depth);
        var chapters = result.getPerspectiveChapters();
        if (chapters != null && chapters.size() > maxPerspectives) {
            List<String> keysToRemove = new ArrayList<>();
            int count = 0;
            for (String key : chapters.keySet()) {
                if (count >= maxPerspectives) {
                    keysToRemove.add(key);
                }
                count++;
            }
            for (String key : keysToRemove) {
                chapters.remove(key);
            }
            ContextMeta meta = result.getContextMeta();
            if (!meta.isTruncated()) {
                meta.setTruncated(true);
            }
        }
    }

    private void collectTruncationInfo(GuidanceResult result, List<PerspectiveResult> results) {
        ContextMeta meta = result.getContextMeta();
        for (PerspectiveResult pr : results) {
            if (pr.isTruncated()) {
                meta.getTruncations().add(new ContextMeta.TruncationNote(
                        pr.getPerspectiveCode(), pr.getTruncatedReason()));
            }
        }
    }
}
