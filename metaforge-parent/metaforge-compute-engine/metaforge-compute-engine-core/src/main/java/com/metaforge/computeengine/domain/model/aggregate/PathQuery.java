package com.metaforge.computeengine.domain.model.aggregate;

import com.metaforge.computeengine.api.enums.AssociationType;
import com.metaforge.computeengine.api.enums.TraversalDirection;
import com.metaforge.computeengine.api.enums.TruncatedReason;
import com.metaforge.computeengine.domain.model.entity.ClosuredEntity;
import com.metaforge.computeengine.domain.model.entity.TraversalPath;
import com.metaforge.computeengine.domain.model.valueobject.FQN;
import com.metaforge.computeengine.domain.model.valueobject.FilterCriteriaVO;
import com.metaforge.computeengine.domain.model.valueobject.TraversalDepth;
import com.metaforge.computeengine.domain.model.valueobject.TransitivityRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 路径推理聚合根。
 *
 * <p>封装单次路径推理——路径搜索、闭包计算、多跳推理、可达性判定。
 * 路径推理仅沿传导规则中 transitive=true 的关系类型展开，多跳推理每步需满足传导兼容性。
 * 循环引用自动去重截断，最大跳跃步数 <= 3。
 *
 * @author metaforge
 */
public class PathQuery {

    private final FQN sourceFqn;
    private final FQN targetFqn;
    private final TraversalDirection direction;
    private final Set<AssociationType> relationTypes;
    private final int maxDepth;
    private final TraversalDepth traversalDepth;
    private final Map<AssociationType, TransitivityRule> transitivityRules;
    private final long timeoutMs;
    private final long startTime;

    private final List<TraversalPath> paths = new ArrayList<>();
    private final Map<Integer, List<ClosuredEntity>> closureLayers = new LinkedHashMap<>();
    private boolean truncated = false;
    private TruncatedReason truncatedReason;
    private boolean reachable = false;

    public PathQuery(FQN sourceFqn, FQN targetFqn, TraversalDirection direction,
                     Set<AssociationType> relationTypes, int maxDepth,
                     TraversalDepth traversalDepth,
                     Map<AssociationType, TransitivityRule> transitivityRules,
                     long timeoutMs) {
        this.sourceFqn = sourceFqn;
        this.targetFqn = targetFqn;
        this.direction = direction != null ? direction : TraversalDirection.BIDIRECTIONAL;
        this.relationTypes = relationTypes != null ? relationTypes : Collections.emptySet();
        this.maxDepth = maxDepth;
        this.traversalDepth = traversalDepth;
        this.transitivityRules = transitivityRules != null ? transitivityRules : Collections.emptyMap();
        this.timeoutMs = timeoutMs;
        this.startTime = System.currentTimeMillis();
    }

    public FQN getSourceFqn() { return sourceFqn; }
    public FQN getTargetFqn() { return targetFqn; }
    public TraversalDirection getDirection() { return direction; }
    public Set<AssociationType> getRelationTypes() { return relationTypes; }
    public int getMaxDepth() { return maxDepth; }
    public TraversalDepth getTraversalDepth() { return traversalDepth; }
    public Map<AssociationType, TransitivityRule> getTransitivityRules() { return transitivityRules; }
    public long getTimeoutMs() { return timeoutMs; }

    public boolean isTimeout() {
        return System.currentTimeMillis() - startTime > timeoutMs;
    }

    public void addPath(TraversalPath path) {
        paths.add(path);
    }

    public List<TraversalPath> getPaths() {
        return Collections.unmodifiableList(paths);
    }

    /**
     * 获取最短路径（按边数最少）。
     */
    public TraversalPath getShortestPath() {
        return paths.stream()
                .min((a, b) -> Integer.compare(a.getSegments().size(), b.getSegments().size()))
                .orElse(null);
    }

    public void addClosureEntity(int depth, ClosuredEntity entity) {
        closureLayers.computeIfAbsent(depth, k -> new ArrayList<>()).add(entity);
    }

    public Map<Integer, List<ClosuredEntity>> getClosureLayers() {
        return Collections.unmodifiableMap(closureLayers);
    }

    public int getTotalReachable() {
        Set<String> unique = new LinkedHashSet<>();
        for (List<ClosuredEntity> layer : closureLayers.values()) {
            for (ClosuredEntity e : layer) {
                unique.add(e.getFqn().getValue());
            }
        }
        return unique.size();
    }

    public boolean isReachable() { return reachable; }
    public void setReachable(boolean reachable) { this.reachable = reachable; }

    public boolean isTruncated() { return truncated; }
    public TruncatedReason getTruncatedReason() { return truncatedReason; }

    public void markTruncated(TruncatedReason reason) {
        this.truncated = true;
        this.truncatedReason = reason;
    }

    /**
     * 判断指定 AssociationType 是否可传递。
     */
    public boolean isTransitive(AssociationType type) {
        TransitivityRule rule = transitivityRules.get(type);
        return rule != null && rule.isTransitive();
    }

    /**
     * 获取有效最大深度。
     */
    public int effectiveDepth(AssociationType type) {
        if (traversalDepth != null) {
            return traversalDepth.effectiveDepth(type);
        }
        return maxDepth;
    }

    /**
     * 计算闭包：在指定深度内，沿可传递关系类型 BFS 扩展。
     */
    public void computeClosure(Set<FQN> seedFqns, int maxDepth, Set<AssociationType> transitiveTypes) {
        closureLayers.clear();
        Set<String> visited = new LinkedHashSet<>();
        java.util.Queue<BfsEntry> queue = new java.util.LinkedList<>();

        for (FQN fqn : seedFqns) {
            visited.add(fqn.getValue());
            ClosuredEntity ce = new ClosuredEntity(fqn, 0, new LinkedHashSet<>());
            addClosureEntity(0, ce);
            queue.add(new BfsEntry(fqn, 0, new LinkedHashSet<>()));
        }

        while (!queue.isEmpty()) {
            BfsEntry current = queue.poll();
            if (isTimeout() || current.depth >= maxDepth) continue;

            for (AssociationType type : transitiveTypes) {
                addClosuredRelations(current, type, visited, queue);
            }
        }
    }

    private void addClosuredRelations(BfsEntry current, AssociationType type,
                                       Set<String> visited, java.util.Queue<BfsEntry> queue) {
        // Placeholder for BFS closure expansion — delegates to PathInferenceService
    }

    /**
     * 多跳遍历：验证每步跳跃序列的合法性。
     */
    public void multiHopTraverse(List<HopDefinition> hopDefinitions) {
        if (hopDefinitions == null || hopDefinitions.isEmpty()) return;
        if (hopDefinitions.size() > 3) {
            markTruncated(TruncatedReason.DEPTH_EXCEEDED);
            return;
        }

        paths.clear();
        for (HopDefinition hop : hopDefinitions) {
            if (!isTransitive(hop.relationType())) {
                markTruncated(TruncatedReason.DEPTH_EXCEEDED);
                return;
            }
        }
    }

    /**
     * 多跳定义。
     */
    public record HopDefinition(AssociationType relationType, TraversalDirection direction) {}

    private record BfsEntry(FQN fqn, int depth, Set<AssociationType> arrivedByTypes) {}

    /**
     * 判定可达性：找到首条路径即早期终止。
     */
    public boolean checkReachability() {
        return reachable;
    }
}
