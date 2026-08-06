package com.metaforge.computeengine.application.service;

import com.metaforge.computeengine.api.dto.response.ClosureResult;
import com.metaforge.computeengine.api.dto.response.ClosureResult.ClosuredEntityDetail;
import com.metaforge.computeengine.api.dto.response.PathResult;
import com.metaforge.computeengine.api.dto.response.PathResult.PathDetail;
import com.metaforge.computeengine.api.dto.response.PathResult.PathStep;
import com.metaforge.computeengine.api.dto.request.ClosureQueryRequest;
import com.metaforge.computeengine.api.dto.request.MultiHopQueryRequest;
import com.metaforge.computeengine.api.dto.request.PathQueryRequest;
import com.metaforge.computeengine.api.dto.request.ReachabilityCheckRequest;
import com.metaforge.computeengine.api.enums.AssociationType;
import com.metaforge.computeengine.api.enums.TruncatedReason;
import com.metaforge.computeengine.api.service.PathReasoningService;
import com.metaforge.computeengine.domain.exception.EntityNotFoundException;
import com.metaforge.computeengine.domain.model.aggregate.PathQuery;
import com.metaforge.computeengine.domain.model.aggregate.PathQuery.HopDefinition;
import com.metaforge.computeengine.domain.model.entity.ClosuredEntity;
import com.metaforge.computeengine.domain.model.entity.TraversalPath;
import com.metaforge.computeengine.domain.model.valueobject.FQN;
import com.metaforge.computeengine.domain.model.valueobject.TraversalDepth;
import com.metaforge.computeengine.domain.model.valueobject.TransitivityRule;
import com.metaforge.computeengine.domain.port.EntityDataPort;
import com.metaforge.computeengine.domain.service.PathInferenceService;
import com.metaforge.computeengine.domain.service.TransitivityRuleService;
import com.metaforge.computeengine.infrastructure.config.ComputeEngineProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 路径推理应用服务实现。
 *
 * <p>实现 PathReasoningService 接口，编排 PathQuery 聚合根与 PathInferenceService 领域服务。
 *
 * @author metaforge
 */
@Service
@Transactional(readOnly = true)
public class PathReasoningServiceImpl implements PathReasoningService {

    private static final Logger log = LoggerFactory.getLogger(PathReasoningServiceImpl.class);

    private final EntityDataPort entityDataPort;
    private final PathInferenceService pathInferenceService;
    private final TransitivityRuleService transitivityRuleService;
    private final ComputeEngineProperties properties;

    public PathReasoningServiceImpl(EntityDataPort entityDataPort,
                                     PathInferenceService pathInferenceService,
                                     TransitivityRuleService transitivityRuleService,
                                     ComputeEngineProperties properties) {
        this.entityDataPort = entityDataPort;
        this.pathInferenceService = pathInferenceService;
        this.transitivityRuleService = transitivityRuleService;
        this.properties = properties;
    }

    @Override
    public PathResult findPaths(PathQueryRequest request) {
        validateEntity(request.sourceFqn());
        validateEntity(request.targetFqn());

        FQN sourceFqn = new FQN(request.sourceFqn());
        FQN targetFqn = new FQN(request.targetFqn());
        Set<AssociationType> types = request.relationTypes();
        TraversalDepth traversalDepth = buildTraversalDepth(types);
        Map<AssociationType, TransitivityRule> rules = transitivityRuleService.getAllRules();

        PathQuery query = new PathQuery(sourceFqn, targetFqn, request.direction(),
                types, request.maxDepth(), traversalDepth, rules,
                properties.getTraversal().getTimeoutMs());

        List<TraversalPath> paths;
        if (request.findShortest()) {
            TraversalPath shortest = pathInferenceService.findShortestPath(query);
            paths = shortest != null ? List.of(shortest) : Collections.emptyList();
        } else {
            paths = pathInferenceService.findAllPaths(query);
        }

        if (query.isTimeout()) {
            query.markTruncated(TruncatedReason.TIMEOUT);
        }

        return buildPathResult(paths, query.isTruncated(), query.getTruncatedReason());
    }

    @Override
    public ClosureResult computeClosure(ClosureQueryRequest request) {
        validateEntity(request.sourceFqn());

        FQN sourceFqn = new FQN(request.sourceFqn());
        Set<AssociationType> types = request.relationTypes();
        TraversalDepth traversalDepth = buildTraversalDepth(types);
        Map<AssociationType, TransitivityRule> rules = transitivityRuleService.getAllRules();

        Set<AssociationType> transitiveTypes = rules.entrySet().stream()
                .filter(e -> e.getValue().isTransitive())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        PathQuery query = new PathQuery(sourceFqn, null, null,
                types, properties.getTraversal().getMaxDepth(), traversalDepth, rules,
                properties.getTraversal().getTimeoutMs());

        pathInferenceService.computeClosure(query, transitiveTypes);

        if (query.isTimeout()) {
            query.markTruncated(TruncatedReason.TIMEOUT);
        }

        Map<Integer, List<ClosuredEntityDetail>> layers = new LinkedHashMap<>();
        Map<AssociationType, Integer> typeStats = new HashMap<>();

        for (var entry : query.getClosureLayers().entrySet()) {
            List<ClosuredEntityDetail> details = entry.getValue().stream()
                    .map(e -> new ClosuredEntityDetail(
                            e.getFqn().getValue(), e.getDepth(), e.getArrivedByTypes()))
                    .toList();
            layers.put(entry.getKey(), details);

            for (ClosuredEntity ce : entry.getValue()) {
                for (AssociationType t : ce.getArrivedByTypes()) {
                    typeStats.merge(t, 1, Integer::sum);
                }
            }
        }

        return new ClosureResult(layers, query.getTotalReachable(), typeStats,
                query.isTruncated(), query.getTruncatedReason());
    }

    @Override
    public PathResult multiHopReasoning(MultiHopQueryRequest request) {
        validateEntity(request.sourceFqn());

        FQN sourceFqn = new FQN(request.sourceFqn());
        Map<AssociationType, TransitivityRule> rules = transitivityRuleService.getAllRules();
        TraversalDepth traversalDepth = buildTraversalDepth(
                request.hopSteps().stream().map(h -> h.relationType()).collect(Collectors.toSet()));

        PathQuery query = new PathQuery(sourceFqn, null, null,
                null, properties.getTraversal().getMaxDepth(), traversalDepth, rules,
                properties.getTraversal().getTimeoutMs());

        List<HopDefinition> hopDefs = request.hopSteps().stream()
                .map(h -> new HopDefinition(h.relationType(), h.direction()))
                .toList();

        List<TraversalPath> paths = pathInferenceService.multiHopTraverse(query, hopDefs);
        paths.forEach(query::addPath);

        if (query.isTimeout()) {
            query.markTruncated(TruncatedReason.TIMEOUT);
        }

        return buildPathResult(query.getPaths(), query.isTruncated(), query.getTruncatedReason());
    }

    @Override
    public PathResult checkReachability(ReachabilityCheckRequest request) {
        validateEntity(request.sourceFqn());
        validateEntity(request.targetFqn());

        FQN sourceFqn = new FQN(request.sourceFqn());
        FQN targetFqn = new FQN(request.targetFqn());
        Map<AssociationType, TransitivityRule> rules = transitivityRuleService.getAllRules();

        PathQuery query = new PathQuery(sourceFqn, targetFqn, null,
                request.relationTypes(), properties.getTraversal().getMaxDepth(),
                null, rules, properties.getTraversal().getTimeoutMs());

        boolean reachable = pathInferenceService.checkReachability(query);

        List<PathDetail> details = reachable
                ? List.of(new PathDetail("reachable", Collections.emptyList(), 0))
                : Collections.emptyList();

        return new PathResult(details, details.size(), query.isTruncated(), query.getTruncatedReason());
    }

    private PathResult buildPathResult(List<TraversalPath> paths, boolean truncated, TruncatedReason reason) {
        List<PathDetail> details = paths.stream().map(p -> {
            List<PathStep> steps = p.getSegments().stream().map(s -> new PathStep(
                    s.getFromEntity().getValue(), s.getToEntity().getValue(),
                    s.getRelation().getValue(), s.getRelationType(),
                    s.getDirection(), s.getWeight())).toList();
            return new PathDetail(p.getPathId(), steps, p.getTotalWeight());
        }).toList();
        return new PathResult(details, details.size(), truncated, reason);
    }

    private void validateEntity(String fqn) {
        var entity = entityDataPort.findByFqn(new FQN(fqn));
        if (entity == null) {
            throw new EntityNotFoundException(fqn);
        }
    }

    private TraversalDepth buildTraversalDepth(Set<AssociationType> types) {
        int globalMax = properties.getTraversal().getMaxDepth();
        if (types == null || types.isEmpty()) return new TraversalDepth(globalMax);
        var perType = new HashMap<AssociationType, Integer>();
        for (var t : types) {
            perType.put(t, transitivityRuleService.getEffectiveMaxDepth(t, globalMax));
        }
        return new TraversalDepth(globalMax, perType);
    }
}
