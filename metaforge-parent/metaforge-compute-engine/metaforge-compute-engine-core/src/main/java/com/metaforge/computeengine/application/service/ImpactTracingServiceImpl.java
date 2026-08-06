package com.metaforge.computeengine.application.service;

import com.metaforge.computeengine.api.dto.common.RelationSummary;
import com.metaforge.computeengine.api.dto.request.ImpactDiffusionRequest;
import com.metaforge.computeengine.api.dto.response.ImpactTraceResult;
import com.metaforge.computeengine.api.dto.response.ImpactTraceResult.ImpactEntityDetail;
import com.metaforge.computeengine.api.enums.AssociationType;
import com.metaforge.computeengine.api.enums.TruncatedReason;
import com.metaforge.computeengine.api.service.ImpactTracingService;
import com.metaforge.computeengine.domain.exception.EntityNotFoundException;
import com.metaforge.computeengine.domain.model.aggregate.ImpactQuery;
import com.metaforge.computeengine.domain.model.entity.ImpactEntity;
import com.metaforge.computeengine.domain.model.entity.TraversalPath;
import com.metaforge.computeengine.domain.model.valueobject.FQN;
import com.metaforge.computeengine.domain.model.valueobject.TraversalDepth;
import com.metaforge.computeengine.domain.port.EntityDataPort;
import com.metaforge.computeengine.domain.port.RelationDataPort;
import com.metaforge.computeengine.domain.service.ImpactAnalysisService;
import com.metaforge.computeengine.domain.service.TransitivityRuleService;
import com.metaforge.computeengine.infrastructure.config.ComputeEngineProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 影响溯源应用服务实现。
 *
 * <p>实现 ImpactTracingService 接口，编排 ImpactQuery 聚合根与 ImpactAnalysisService 领域服务。
 *
 * @author metaforge
 */
@Service
@Transactional(readOnly = true)
public class ImpactTracingServiceImpl implements ImpactTracingService {

    private static final Logger log = LoggerFactory.getLogger(ImpactTracingServiceImpl.class);

    private final EntityDataPort entityDataPort;
    private final RelationDataPort relationDataPort;
    private final ImpactAnalysisService impactAnalysisService;
    private final TransitivityRuleService transitivityRuleService;
    private final ComputeEngineProperties properties;

    public ImpactTracingServiceImpl(EntityDataPort entityDataPort,
                                     RelationDataPort relationDataPort,
                                     ImpactAnalysisService impactAnalysisService,
                                     TransitivityRuleService transitivityRuleService,
                                     ComputeEngineProperties properties) {
        this.entityDataPort = entityDataPort;
        this.relationDataPort = relationDataPort;
        this.impactAnalysisService = impactAnalysisService;
        this.transitivityRuleService = transitivityRuleService;
        this.properties = properties;
    }

    @Override
    public ImpactTraceResult diffuseForward(ImpactDiffusionRequest request) {
        validateEntity(request.centerFqn());

        FQN centerFqn = new FQN(request.centerFqn());
        TraversalDepth traversalDepth = buildTraversalDepth(request.relationTypes());

        ImpactQuery query = new ImpactQuery(centerFqn, request.direction(),
                request.relationTypes(), request.maxDepth(), traversalDepth,
                properties.getTraversal().getTimeoutMs());

        impactAnalysisService.diffuse(query);

        if (query.isTimeout()) query.markTruncated(TruncatedReason.TIMEOUT);
        return buildResult(query);
    }

    @Override
    public ImpactTraceResult traceBackward(ImpactDiffusionRequest request) {
        validateEntity(request.centerFqn());

        FQN centerFqn = new FQN(request.centerFqn());
        TraversalDepth traversalDepth = buildTraversalDepth(request.relationTypes());

        ImpactQuery query = new ImpactQuery(centerFqn, request.direction(),
                request.relationTypes(), request.maxDepth(), traversalDepth,
                properties.getTraversal().getTimeoutMs());

        impactAnalysisService.trace(query);

        if (query.isTimeout()) query.markTruncated(TruncatedReason.TIMEOUT);
        return buildResult(query);
    }

    @Override
    public ImpactTraceResult getImpactPaths(String sourceFqn, String targetFqn,
                                              List<AssociationType> relationTypes, int maxDepth) {
        validateEntity(sourceFqn);
        validateEntity(targetFqn);

        FQN source = new FQN(sourceFqn);
        FQN target = new FQN(targetFqn);
        Set<AssociationType> types = relationTypes != null && !relationTypes.isEmpty()
                ? Set.copyOf(relationTypes) : null;

        List<TraversalPath> paths = impactAnalysisService.impactPaths(
                source, target, types, maxDepth, properties.getTraversal().getTimeoutMs());

        List<TraversalPath> sortedPaths = paths.stream()
                .sorted(Comparator.comparingInt(p -> p.getSegments().size()))
                .toList();

        List<ImpactEntityDetail> entities = sortedPaths.stream()
                .flatMap(p -> p.getSegments().stream())
                .map(s -> new ImpactEntityDetail(s.getToEntity().getValue(), 0, Set.of(s.getRelationType())))
                .distinct()
                .collect(Collectors.toList());

        List<RelationSummary> relations = sortedPaths.stream()
                .flatMap(p -> p.getSegments().stream())
                .map(s -> new RelationSummary(s.getRelation().getValue(), s.getRelationType(),
                        s.getFromEntity().getValue(), s.getToEntity().getValue()))
                .distinct()
                .collect(Collectors.toList());

        return new ImpactTraceResult(entities.size(), Map.of(),
                Map.of(), entities, relations, false, null);
    }

    private ImpactTraceResult buildResult(ImpactQuery query) {
        Map<String, Integer> typeStats = new HashMap<>();
        for (ImpactEntity entity : query.getAllEntities()) {
            for (AssociationType type : entity.getAffectedByTypes()) {
                typeStats.merge(type.name(), 1, Integer::sum);
            }
        }

        Map<Integer, List<ImpactEntityDetail>> layers = new LinkedHashMap<>();
        for (var entry : query.getLayerStats().entrySet()) {
            layers.put(entry.getKey(), entry.getValue().stream()
                    .map(e -> new ImpactEntityDetail(e.getFqn().getValue(), e.getDepth(), e.getAffectedByTypes()))
                    .toList());
        }

        List<ImpactEntityDetail> entityList = query.getAllEntities().stream()
                .map(e -> new ImpactEntityDetail(e.getFqn().getValue(), e.getDepth(), e.getAffectedByTypes()))
                .toList();

        return new ImpactTraceResult(query.getTotalImpacted(), layers, typeStats,
                entityList, List.of(), query.isTruncated(), query.getTruncatedReason());
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
