package com.metaforge.computeengine.domain.model.aggregate;

import com.metaforge.computeengine.api.enums.AssociationType;
import com.metaforge.computeengine.api.enums.TraversalDirection;
import com.metaforge.computeengine.api.enums.TruncatedReason;
import com.metaforge.computeengine.domain.model.entity.ImpactEntity;
import com.metaforge.computeengine.domain.model.entity.TraversalPath;
import com.metaforge.computeengine.domain.model.valueobject.FQN;
import com.metaforge.computeengine.domain.model.valueobject.TraversalDepth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 影响溯源聚合根。
 *
 * <p>封装单次影响溯源操作——正向影响扩散（BFS 沿出边）、反向依赖溯源（BFS 沿入边）、
 * 影响路径详情查询。沿指定关系类型 BFS 扩展，按层级分组，同一实体被多路径影响时仅统计一次。
 *
 * @author metaforge
 */
public class ImpactQuery {

    private final FQN centerFqn;
    private final TraversalDirection direction;
    private final Set<AssociationType> relationTypes;
    private final int maxDepth;
    private final TraversalDepth traversalDepth;
    private final long timeoutMs;
    private final long startTime;

    private final Map<Integer, List<ImpactEntity>> layerStats = new LinkedHashMap<>();
    private final Set<ImpactEntity> allEntities = new LinkedHashSet<>();
    private final List<TraversalPath> impactPaths = new ArrayList<>();
    private boolean truncated = false;
    private TruncatedReason truncatedReason;

    public ImpactQuery(FQN centerFqn, TraversalDirection direction,
                       Set<AssociationType> relationTypes, int maxDepth,
                       TraversalDepth traversalDepth, long timeoutMs) {
        this.centerFqn = centerFqn;
        this.direction = direction != null ? direction : TraversalDirection.FORWARD;
        this.relationTypes = relationTypes != null ? relationTypes : Collections.emptySet();
        this.maxDepth = maxDepth;
        this.traversalDepth = traversalDepth;
        this.timeoutMs = timeoutMs;
        this.startTime = System.currentTimeMillis();
    }

    public FQN getCenterFqn() { return centerFqn; }
    public TraversalDirection getDirection() { return direction; }
    public Set<AssociationType> getRelationTypes() { return relationTypes; }
    public int getMaxDepth() { return maxDepth; }
    public TraversalDepth getTraversalDepth() { return traversalDepth; }
    public long getTimeoutMs() { return timeoutMs; }
    public boolean isTimeout() { return System.currentTimeMillis() - startTime > timeoutMs; }

    public Map<Integer, List<ImpactEntity>> getLayerStats() { return Collections.unmodifiableMap(layerStats); }
    public Set<ImpactEntity> getAllEntities() { return Collections.unmodifiableSet(allEntities); }
    public List<TraversalPath> getImpactPaths() { return Collections.unmodifiableList(impactPaths); }
    public boolean isTruncated() { return truncated; }
    public TruncatedReason getTruncatedReason() { return truncatedReason; }
    public void markTruncated(TruncatedReason reason) { this.truncated = true; this.truncatedReason = reason; }

    /**
     * 正向影响扩散——沿出边 BFS 扩展。
     */
    public void diffuseForward() {
        // 委托 ImpactAnalysisService 执行
    }

    /**
     * 反向依赖溯源——沿入边 BFS 追溯。
     */
    public void traceBackward() {
        // 委托 ImpactAnalysisService 执行
    }

    /**
     * 添加影响实体到指定层级。
     */
    public void addImpactEntity(int depth, ImpactEntity entity) {
        layerStats.computeIfAbsent(depth, k -> new ArrayList<>()).add(entity);
        allEntities.add(entity);
    }

    /**
     * 添加影响路径。
     */
    public void addImpactPath(TraversalPath path) {
        impactPaths.add(path);
    }

    /**
     * 总影响实体数。
     */
    public int getTotalImpacted() {
        return allEntities.size();
    }
}
