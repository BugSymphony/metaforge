package com.metaforge.computeengine.application.service;

import com.metaforge.common.dto.PageResult;
import com.metaforge.computeengine.api.dto.common.EntitySummary;
import com.metaforge.computeengine.api.dto.common.FilterCriteria;
import com.metaforge.computeengine.api.dto.common.RelationSummary;
import com.metaforge.computeengine.api.dto.request.AdjacencyQueryRequest;
import com.metaforge.computeengine.api.dto.request.BatchQueryRequest;
import com.metaforge.computeengine.api.dto.request.CompositionTreeQueryRequest;
import com.metaforge.computeengine.api.dto.request.CompoundSearchRequest;
import com.metaforge.computeengine.api.dto.request.PatternMatchRequest;
import com.metaforge.computeengine.api.dto.request.SubgraphQueryRequest;
import com.metaforge.computeengine.api.dto.response.GraphQueryResult;
import com.metaforge.computeengine.api.enums.AssociationType;
import com.metaforge.computeengine.api.enums.MatchMode;
import com.metaforge.computeengine.api.enums.TruncatedReason;
import com.metaforge.computeengine.api.enums.TraversalDirection;
import com.metaforge.computeengine.api.service.GraphQueryService;
import com.metaforge.computeengine.domain.exception.BatchSizeExceededException;
import com.metaforge.computeengine.domain.exception.EntityNotFoundException;
import com.metaforge.computeengine.domain.exception.InvalidPatternException;
import com.metaforge.computeengine.domain.model.aggregate.GraphQuery;
import com.metaforge.computeengine.domain.model.valueobject.FQN;
import com.metaforge.computeengine.domain.model.valueobject.FilterCriteriaVO;
import com.metaforge.computeengine.domain.model.valueobject.GraphPattern;
import com.metaforge.computeengine.domain.model.valueobject.PatternSegment;
import com.metaforge.computeengine.domain.model.valueobject.RelationSnapshot;
import com.metaforge.computeengine.domain.model.valueobject.TraversalDepth;
import com.metaforge.computeengine.domain.port.EntityDataPort;
import com.metaforge.computeengine.domain.port.RelationDataPort;
import com.metaforge.computeengine.domain.service.FilterPredicateService;
import com.metaforge.computeengine.domain.service.GraphTraversalService;
import com.metaforge.computeengine.domain.service.TransitivityRuleService;
import com.metaforge.computeengine.infrastructure.config.ComputeEngineProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 多维图查询应用服务实现。
 *
 * <p>实现 GraphQueryService 接口，编排图查询聚合根、图遍历服务与过滤服务。
 * 每个方法：验证输入 → 构建 GraphQuery → 执行遍历 → 组装结果含截断标记。
 *
 * @author metaforge
 */
@Service
@Transactional(readOnly = true)
public class GraphQueryServiceImpl implements GraphQueryService {

    private static final Logger log = LoggerFactory.getLogger(GraphQueryServiceImpl.class);
    private static final int MAX_BATCH_SIZE = 200;

    private final EntityDataPort entityDataPort;
    private final RelationDataPort relationDataPort;
    private final GraphTraversalService graphTraversalService;
    private final FilterPredicateService filterPredicateService;
    private final TransitivityRuleService transitivityRuleService;
    private final ComputeEngineProperties properties;

    public GraphQueryServiceImpl(EntityDataPort entityDataPort,
                                  RelationDataPort relationDataPort,
                                  GraphTraversalService graphTraversalService,
                                  FilterPredicateService filterPredicateService,
                                  TransitivityRuleService transitivityRuleService,
                                  ComputeEngineProperties properties) {
        this.entityDataPort = entityDataPort;
        this.relationDataPort = relationDataPort;
        this.graphTraversalService = graphTraversalService;
        this.filterPredicateService = filterPredicateService;
        this.transitivityRuleService = transitivityRuleService;
        this.properties = properties;
    }

    @Override
    public GraphQueryResult queryAdjacency(AdjacencyQueryRequest request) {
        validateSourceEntity(request.sourceFqn());

        FQN sourceFqn = new FQN(request.sourceFqn());
        FilterCriteriaVO filterVO = convertFilter(request.filterCriteria());
        TraversalDepth traversalDepth = buildTraversalDepth(request.relationTypes());

        GraphQuery query = new GraphQuery(sourceFqn, request.direction(), request.maxDepth(),
                request.relationTypes(), filterVO, traversalDepth,
                properties.getTraversal().getMaxResultCount(), properties.getTraversal().getTimeoutMs());

        graphTraversalService.executeBfs(query);
        query.computeAdjacency();

        if (query.isTimeout()) {
            query.markTruncated(TruncatedReason.TIMEOUT);
        }

        return query.assembleResult(null);
    }

    @Override
    public GraphQueryResult queryCompositionTree(CompositionTreeQueryRequest request) {
        validateSourceEntity(request.rootFqn());

        FQN rootFqn = new FQN(request.rootFqn());
        FilterCriteriaVO filterVO = convertFilter(request.filterCriteria());
        Set<AssociationType> compTypes = Set.of(AssociationType.COMPOSITION);
        TraversalDepth traversalDepth = buildTraversalDepth(compTypes);

        GraphQuery query = new GraphQuery(rootFqn, request.direction(), request.maxDepth(),
                compTypes, filterVO, traversalDepth,
                properties.getTraversal().getMaxResultCount(), properties.getTraversal().getTimeoutMs());

        graphTraversalService.executeBfs(query);
        query.computeAdjacency();

        if (query.isTimeout()) {
            query.markTruncated(TruncatedReason.TIMEOUT);
        }

        return query.assembleResult(null);
    }

    @Override
    public GraphQueryResult querySubgraph(SubgraphQueryRequest request) {
        for (String fqn : request.centerFqns()) {
            validateSourceEntity(fqn);
        }

        FilterCriteriaVO filterVO = convertFilter(request.filterCriteria());
        var traversalDepth = new TraversalDepth(Math.min(request.maxDepth(), properties.getTraversal().getMaxDepth()));

        GraphQueryResult mergedResult = null;
        for (String centerFqn : request.centerFqns()) {
            FQN fqn = new FQN(centerFqn);
            GraphQuery query = new GraphQuery(fqn, TraversalDirection.BIDIRECTIONAL, request.maxDepth(),
                    null, filterVO, traversalDepth,
                    properties.getTraversal().getMaxResultCount(), properties.getTraversal().getTimeoutMs());

            graphTraversalService.executeBfs(query);
            query.computeAdjacency();

            if (mergedResult == null) {
                mergedResult = query.assembleResult(null);
            } else {
                GraphQueryResult subResult = query.assembleResult(null);
                mergedResult = mergeResults(mergedResult, subResult);
            }
        }

        return mergedResult != null ? mergedResult : emptyResult();
    }

    @Override
    public GraphQueryResult queryPatternMatch(PatternMatchRequest request) {
        try {
            GraphPattern pattern = new GraphPattern(request.pattern());
            if (!pattern.isValid()) {
                throw new InvalidPatternException(request.pattern(), "模式无效或路径段数超限");
            }
            int maxResults = request.maxResults() > 0 ? request.maxResults() : 100;

            Set<com.metaforge.computeengine.domain.model.valueobject.EntitySnapshot> entities = new LinkedHashSet<>();
            Set<RelationSnapshot> relations = new LinkedHashSet<>();
            List<PatternSegment> segments = pattern.getSegments();
            int matched = 0;

            List<com.metaforge.computeengine.domain.model.valueobject.EntitySnapshot> starts =
                    entityDataPort.findByFqnPrefixes(List.of(""), 10000);
            for (var start : starts) {
                if (matched >= maxResults) break;
                matched += matchPatternFrom(start, segments, 0, entities, relations, maxResults - matched);
            }

            var adjacencyMap = new LinkedHashMap<String, List<String>>();
            for (var e : entities) {
                List<String> related = new ArrayList<>();
                for (var r : relations) {
                    if (e.getFqn().equals(r.getSourceEntityFqn())
                            || e.getFqn().equals(r.getTargetEntityFqn())) {
                        related.add(r.getFqn().getValue());
                    }
                }
                adjacencyMap.put(e.getFqn().getValue(), related);
            }

            return new GraphQueryResult(
                    entities.stream()
                            .map(e -> new EntitySummary(e.getFqn().getValue(), e.getName(),
                                    e.getEntitySchemaFqn() != null ? e.getEntitySchemaFqn().getValue() : null))
                            .toList(),
                    relations.stream()
                            .map(r -> new RelationSummary(r.getFqn().getValue(), r.getAssociationType(),
                                    r.getSourceEntityFqn() != null ? r.getSourceEntityFqn().getValue() : null,
                                    r.getTargetEntityFqn() != null ? r.getTargetEntityFqn().getValue() : null))
                            .toList(),
                    adjacencyMap,
                    false, null, null);
        } catch (IllegalArgumentException e) {
            throw new InvalidPatternException(request.pattern(), e.getMessage());
        }
    }

    private int matchPatternFrom(com.metaforge.computeengine.domain.model.valueobject.EntitySnapshot current,
                                 List<PatternSegment> segments, int idx,
                                 Set<com.metaforge.computeengine.domain.model.valueobject.EntitySnapshot> entities,
                                 Set<RelationSnapshot> relations, int budget) {
        if (idx >= segments.size() || budget <= 0) return 0;
        PatternSegment seg = segments.get(idx);
        if (!matchesEntityType(current, seg.sourceEntityType())) return 0;

        int count = 0;
        List<com.metaforge.computeengine.domain.model.valueobject.RelationSnapshot> outRels =
                relationDataPort.findOutboundRelations(current.getFqn(), null, 100);
        for (var rel : outRels) {
            if (budget <= 0) break;
            if (!matchesRelationType(rel, seg.relationType())) continue;
            var target = rel.getTargetEntityFqn();
            if (target == null) continue;
            var targetEntity = entityDataPort.findByFqn(target);
            if (targetEntity == null || !matchesEntityType(targetEntity, seg.targetEntityType())) continue;

            entities.add(current);
            entities.add(targetEntity);
            relations.add(rel);
            if (idx == segments.size() - 1) {
                count++;
                budget--;
            } else {
                int m = matchPatternFrom(targetEntity, segments, idx + 1, entities, relations, budget);
                count += m;
                budget -= m;
            }
        }
        return count;
    }

    private boolean matchesEntityType(com.metaforge.computeengine.domain.model.valueobject.EntitySnapshot entity, String pattern) {
        if ("*".equals(pattern)) return true;
        if (entity.getEntitySchemaFqn() == null) return false;
        return entity.getEntitySchemaFqn().getValue().equals(pattern);
    }

    private boolean matchesRelationType(com.metaforge.computeengine.domain.model.valueobject.RelationSnapshot relation, String pattern) {
        if ("?".equals(pattern)) return true;
        if (relation.getRelationSchemaFqn() == null) return false;
        return relation.getRelationSchemaFqn().getValue().equals(pattern);
    }

    @Override
    public PageResult<EntitySummary> searchCompound(CompoundSearchRequest request) {
        List<EntitySummary> matched = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        List<com.metaforge.computeengine.domain.model.valueobject.EntitySnapshot> candidates = new ArrayList<>();
        if (request.entityTypes() != null && !request.entityTypes().isEmpty()) {
            for (String type : request.entityTypes()) {
                candidates.addAll(entityDataPort.findByEntitySchemaFqn(type, 10000));
            }
        } else {
            candidates.addAll(entityDataPort.findByFqnPrefixes(List.of(""), 10000));
        }

        for (var entity : candidates) {
            if (!seen.add(entity.getFqn().getValue())) continue;
            if (request.attributes() != null && !request.attributes().isEmpty()
                    && !matchAttributes(entity, request.attributes())) continue;
            if (request.relationTypes() != null && !request.relationTypes().isEmpty()
                    && !matchRelationTypes(entity, request.relationTypes())) continue;
            matched.add(new EntitySummary(entity.getFqn().getValue(), entity.getName(),
                    entity.getEntitySchemaFqn() != null ? entity.getEntitySchemaFqn().getValue() : null));
        }

        if (request.sortField() != null && !request.sortField().isBlank()) {
            final boolean desc = "DESC".equalsIgnoreCase(request.sortField() != null
                    ? request.sortDirection() : null);
            matched.sort((a, b) -> {
                Object va = contentValue(a.fqn(), request.sortField());
                Object vb = contentValue(b.fqn(), request.sortField());
                int c = compareValues(va, vb);
                return desc ? -c : c;
            });
        }

        int page = Math.max(0, request.page());
        int size = request.size() > 0 ? request.size() : 20;
        int from = page * size;
        int to = Math.min(matched.size(), from + size);
        List<EntitySummary> pageContent = from >= matched.size()
                ? Collections.emptyList() : matched.subList(from, to);
        return new PageResult<>(pageContent, matched.size(), page, size);
    }

    private boolean matchAttributes(com.metaforge.computeengine.domain.model.valueobject.EntitySnapshot entity,
                                    List<CompoundSearchRequest.AttributeCondition> attributes) {
        for (var attr : attributes) {
            Object value = entity.getContent() != null ? entity.getContent().get(attr.field()) : null;
            if (!matchValue(value, attr.operator(), attr.value())) return false;
        }
        return true;
    }

    private boolean matchValue(Object actual, String operator, String expected) {
        if (actual == null) return false;
        String op = operator == null ? "EQ" : operator.toUpperCase();
        return switch (op) {
            case "EQ" -> actual.toString().equals(expected);
            case "NEQ" -> !actual.toString().equals(expected);
            case "LIKE" -> actual.toString().contains(expected);
            case "GT" -> compareValues(actual, expected) > 0;
            case "LT" -> compareValues(actual, expected) < 0;
            case "GTE" -> compareValues(actual, expected) >= 0;
            case "LTE" -> compareValues(actual, expected) <= 0;
            default -> false;
        };
    }

    private boolean matchRelationTypes(com.metaforge.computeengine.domain.model.valueobject.EntitySnapshot entity,
                                       List<AssociationType> relationTypes) {
        var types = new java.util.HashSet<>(relationTypes);
        return !relationDataPort.findRelations(entity.getFqn(), TraversalDirection.BIDIRECTIONAL, types, 1).isEmpty();
    }

    private Object contentValue(String fqn, String field) {
        var snapshot = entityDataPort.findByFqn(new FQN(fqn));
        if (snapshot == null || snapshot.getContent() == null) return null;
        return snapshot.getContent().get(field);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private int compareValues(Object a, Object b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        try {
            double da = Double.parseDouble(a.toString());
            double db = Double.parseDouble(b.toString());
            return Double.compare(da, db);
        } catch (NumberFormatException e) {
            if (a instanceof Comparable && a.getClass().equals(b.getClass())) {
                return ((Comparable) a).compareTo(b);
            }
            return a.toString().compareTo(b.toString());
        }
    }

    @Override
    public GraphQueryResult queryBatch(BatchQueryRequest request) {
        if (request.fqns().size() > MAX_BATCH_SIZE) {
            throw new BatchSizeExceededException(request.fqns().size(), MAX_BATCH_SIZE);
        }

        List<FQN> fqns = request.fqns().stream().map(FQN::new).toList();
        var snapshots = entityDataPort.batchFindByFqns(fqns);

        List<String> foundFqns = snapshots.stream()
                .map(s -> s.getFqn().getValue()).toList();
        List<String> notFoundFqns = new ArrayList<>(request.fqns());
        notFoundFqns.removeAll(foundFqns);

        return new GraphQueryResult(
                snapshots.stream()
                        .map(s -> new EntitySummary(
                                s.getFqn().getValue(), s.getName(),
                                s.getEntitySchemaFqn() != null ? s.getEntitySchemaFqn().getValue() : null))
                        .toList(),
                Collections.emptyList(),
                Collections.emptyMap(),
                false, null,
                notFoundFqns.isEmpty() ? null : notFoundFqns
        );
    }

    private void validateSourceEntity(String fqn) {
        var entity = entityDataPort.findByFqn(new FQN(fqn));
        if (entity == null) {
            throw new EntityNotFoundException(fqn);
        }
    }

    private FilterCriteriaVO convertFilter(FilterCriteria filter) {
        if (filter == null) return null;
        return new FilterCriteriaVO(
                filter.associationTypes(),
                filter.sourceFqns(),
                filter.targetFqns(),
                filter.relationInstanceFqns(),
                filter.entityTypes(),
                filter.relationTypes(),
                filter.propertyFilters()
        );
    }

    private TraversalDepth buildTraversalDepth(java.util.Set<AssociationType> types) {
        int globalMax = properties.getTraversal().getMaxDepth();
        if (types == null || types.isEmpty()) {
            return new TraversalDepth(globalMax);
        }
        var perType = new java.util.HashMap<AssociationType, Integer>();
        for (var type : types) {
            perType.put(type, transitivityRuleService.getEffectiveMaxDepth(type, globalMax));
        }
        return new TraversalDepth(globalMax, perType);
    }

    private GraphQueryResult mergeResults(GraphQueryResult a, GraphQueryResult b) {
        List<EntitySummary> mergedEntities = new ArrayList<>(a.entities());
        for (var e : b.entities()) {
            if (mergedEntities.stream().noneMatch(m -> m.fqn().equals(e.fqn()))) {
                mergedEntities.add(e);
            }
        }
        List<RelationSummary> mergedRels = new ArrayList<>(a.relations());
        for (var r : b.relations()) {
            if (mergedRels.stream().noneMatch(m -> m.fqn().equals(r.fqn()))) {
                mergedRels.add(r);
            }
        }
        var mergedAdj = new java.util.LinkedHashMap<>(a.adjacencyMap());
        b.adjacencyMap().forEach((key, value) -> mergedAdj.merge(key, value, (v1, v2) -> {
            List<String> combined = new ArrayList<>(v1);
            v2.forEach(e -> { if (!combined.contains(e)) combined.add(e); });
            return combined;
        }));
        return new GraphQueryResult(mergedEntities, mergedRels, mergedAdj,
                a.truncated() || b.truncated(),
                a.truncatedReason() != null ? a.truncatedReason() : b.truncatedReason(),
                null);
    }

    private GraphQueryResult emptyResult() {
        return new GraphQueryResult(
                Collections.emptyList(), Collections.emptyList(),
                Collections.emptyMap(), false, null, null);
    }
}
