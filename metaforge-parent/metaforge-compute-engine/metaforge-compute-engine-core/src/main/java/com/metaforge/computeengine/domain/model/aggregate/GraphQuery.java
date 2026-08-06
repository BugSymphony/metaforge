package com.metaforge.computeengine.domain.model.aggregate;

import com.metaforge.computeengine.api.dto.common.EntitySummary;
import com.metaforge.computeengine.api.dto.common.RelationSummary;
import com.metaforge.computeengine.api.dto.response.GraphQueryResult;
import com.metaforge.computeengine.api.enums.AssociationType;
import com.metaforge.computeengine.api.enums.TruncatedReason;
import com.metaforge.computeengine.api.enums.TraversalDirection;
import com.metaforge.computeengine.domain.model.valueobject.EntitySnapshot;
import com.metaforge.computeengine.domain.model.valueobject.FQN;
import com.metaforge.computeengine.domain.model.valueobject.FilterCriteriaVO;
import com.metaforge.computeengine.domain.model.valueobject.RelationSnapshot;
import com.metaforge.computeengine.domain.model.valueobject.TraversalDepth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 图查询聚合根。
 *
 * <p>封装单次图查询的完整生命周期——接收查询参数、执行图遍历、收集结果、应用过滤、组装输出。
 * 遍历深度受 TraversalDepth 约束（全局 + per-type），同一实体多路径出现时仅返回一次。
 * 超时/深度超限/数量超限返回截断标记。
 *
 * @author metaforge
 */
public class GraphQuery {

    private final FQN sourceFqn;
    private final TraversalDirection direction;
    private final int maxDepth;
    private final Set<AssociationType> relationTypes;
    private final FilterCriteriaVO filterCriteria;
    private final TraversalDepth traversalDepth;
    private final int maxResultCount;
    private final long startTime;
    private final long timeoutMs;

    private Set<EntitySnapshot> entities = new LinkedHashSet<>();
    private Set<RelationSnapshot> relations = new LinkedHashSet<>();
    private Map<String, List<String>> adjacencyMap = new LinkedHashMap<>();
    private boolean truncated = false;
    private TruncatedReason truncatedReason;

    public GraphQuery(FQN sourceFqn, TraversalDirection direction, int maxDepth,
                      Set<AssociationType> relationTypes, FilterCriteriaVO filterCriteria,
                      TraversalDepth traversalDepth, int maxResultCount, long timeoutMs) {
        this.sourceFqn = sourceFqn;
        this.direction = direction;
        this.maxDepth = maxDepth;
        this.relationTypes = relationTypes != null ? relationTypes : Collections.emptySet();
        this.filterCriteria = filterCriteria;
        this.traversalDepth = traversalDepth;
        this.maxResultCount = maxResultCount;
        this.timeoutMs = timeoutMs;
        this.startTime = System.currentTimeMillis();
    }

    public FQN getSourceFqn() {
        return sourceFqn;
    }

    public TraversalDirection getDirection() {
        return direction;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public Set<AssociationType> getRelationTypes() {
        return relationTypes;
    }

    public FilterCriteriaVO getFilterCriteria() {
        return filterCriteria;
    }

    public TraversalDepth getTraversalDepth() {
        return traversalDepth;
    }

    public int getMaxResultCount() {
        return maxResultCount;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    /**
     * 收集遍历过程中发现的实体。
     */
    public void collectEntity(EntitySnapshot entity) {
        if (!entities.contains(entity)) {
            entities.add(entity);
        }
    }

    /**
     * 收集遍历过程中发现的关系。
     */
    public void collectRelation(RelationSnapshot relation) {
        if (!relations.contains(relation)) {
            relations.add(relation);
        }
    }

    /**
     * 构建实体-关系邻接映射。
     */
    public void computeAdjacency() {
        adjacencyMap = new LinkedHashMap<>();
        for (EntitySnapshot entity : entities) {
            List<String> related = new ArrayList<>();
            for (RelationSnapshot relation : relations) {
                if (entity.getFqn().equals(relation.getSourceEntityFqn())
                        || entity.getFqn().equals(relation.getTargetEntityFqn())) {
                    related.add(relation.getFqn().getValue());
                }
            }
            adjacencyMap.put(entity.getFqn().getValue(), related);
        }
    }

    /**
     * 检查是否已超时。
     */
    public boolean isTimeout() {
        return System.currentTimeMillis() - startTime > timeoutMs;
    }

    /**
     * 检查结果数量是否已超限。
     */
    public boolean isResultCountExceeded() {
        return entities.size() > maxResultCount;
    }

    /**
     * 标记为截断。
     */
    public void markTruncated(TruncatedReason reason) {
        this.truncated = true;
        this.truncatedReason = reason;
    }

    /**
     * 判断指定实体是否符合过滤条件。
     * 在遍历过程中实时生效，被过滤内容不参与遍历且不计入深度。
     */
    public boolean applyFilters(EntitySnapshot entity, RelationSnapshot relation) {
        if (filterCriteria == null || filterCriteria.isEmpty()) {
            return true;
        }
        if (entity != null && !isEntityMatchFilters(entity)) {
            return false;
        }
        return relation == null || isRelationMatchFilters(relation);
    }

    private boolean isEntityMatchFilters(EntitySnapshot entity) {
        if (filterCriteria.hasEntityTypes()) {
            FQN schemaFqn = entity.getEntitySchemaFqn();
            if (schemaFqn == null) {
                return false;
            }
            if (!matchesAnyFqnGroup(schemaFqn.getValue(), filterCriteria.getEntityTypes())) {
                return false;
            }
        }
        return true;
    }

    private boolean isRelationMatchFilters(RelationSnapshot relation) {
        if (filterCriteria.hasAssociationTypes()) {
            if (!filterCriteria.getAssociationTypes().contains(relation.getAssociationType())) {
                return false;
            }
        }
        if (filterCriteria.hasSourceFqns()) {
            FQN sourceFqn = relation.getSourceEntityFqn();
            if (sourceFqn == null || !matchesAnyFqnGroup(sourceFqn.getValue(), filterCriteria.getSourceFqns())) {
                return false;
            }
        }
        if (filterCriteria.hasTargetFqns()) {
            FQN targetFqn = relation.getTargetEntityFqn();
            if (targetFqn == null || !matchesAnyFqnGroup(targetFqn.getValue(), filterCriteria.getTargetFqns())) {
                return false;
            }
        }
        if (filterCriteria.hasRelationInstanceFqns()) {
            if (!matchesAnyFqnGroup(relation.getFqn().getValue(), filterCriteria.getRelationInstanceFqns())) {
                return false;
            }
        }
        if (filterCriteria.hasRelationTypes()) {
            FQN schemaFqn = relation.getRelationSchemaFqn();
            if (schemaFqn == null || !matchesAnyFqnGroup(schemaFqn.getValue(), filterCriteria.getRelationTypes())) {
                return false;
            }
        }
        if (filterCriteria.hasPropertyFilters()) {
            Map<String, Object> content = relation.getContent();
            if (content == null) {
                return false;
            }
            for (var filter : filterCriteria.getPropertyFilters()) {
                Object value = content.get(filter.field());
                if (value == null || !filter.value().equals(String.valueOf(value))) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean matchesAnyFqnGroup(String fqn, List<com.metaforge.computeengine.api.dto.common.FilterCriteria.FqnFilterGroup> groups) {
        if (fqn == null || groups == null || groups.isEmpty()) {
            return false;
        }
        for (var group : groups) {
            if (matchesFqn(fqn, group.value(), group.matchMode())) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesFqn(String fqn, String pattern, com.metaforge.computeengine.api.enums.MatchMode matchMode) {
        if (fqn == null || pattern == null) return false;
        return switch (matchMode) {
            case EXACT -> fqn.equals(pattern);
            case PREFIX -> fqn.startsWith(pattern);
            case PATTERN -> fqn.matches(pattern.replace("_", ".").replace("%", ".*"));
        };
    }

    /**
     * 组装最终查询结果。
     */
    public GraphQueryResult assembleResult(List<String> notFoundFqns) {
        List<EntitySummary> entitySummaries = entities.stream()
                .map(e -> new EntitySummary(
                        e.getFqn().getValue(),
                        e.getName(),
                        e.getEntitySchemaFqn() != null ? e.getEntitySchemaFqn().getValue() : null))
                .toList();

        List<RelationSummary> relationSummaries = relations.stream()
                .map(r -> new RelationSummary(
                        r.getFqn().getValue(),
                        r.getAssociationType(),
                        r.getSourceEntityFqn() != null ? r.getSourceEntityFqn().getValue() : null,
                        r.getTargetEntityFqn() != null ? r.getTargetEntityFqn().getValue() : null))
                .toList();

        return new GraphQueryResult(
                entitySummaries,
                relationSummaries,
                Collections.unmodifiableMap(adjacencyMap),
                truncated,
                truncatedReason,
                notFoundFqns != null ? Collections.unmodifiableList(notFoundFqns) : null
        );
    }
}
