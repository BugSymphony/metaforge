package com.metaforge.computeengine.domain.service;

import com.metaforge.computeengine.api.enums.AssociationType;
import com.metaforge.computeengine.api.enums.TraversalDirection;
import com.metaforge.computeengine.api.enums.TruncatedReason;
import com.metaforge.computeengine.domain.model.aggregate.GraphQuery;
import com.metaforge.computeengine.domain.model.entity.TraversalPath;
import com.metaforge.computeengine.domain.model.valueobject.EntitySnapshot;
import com.metaforge.computeengine.domain.model.valueobject.FQN;
import com.metaforge.computeengine.domain.model.valueobject.PathSegmentVO;
import com.metaforge.computeengine.domain.model.valueobject.RelationSnapshot;
import com.metaforge.computeengine.domain.port.EntityDataPort;
import com.metaforge.computeengine.domain.port.RelationDataPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 图遍历领域服务。
 *
 * <p>通过 BFS 或 DFS 策略执行图遍历，支持深度约束、循环检测、关系类型过滤。
 * BFS 用于邻接查询与子图扩展，DFS 用于路径枚举。
 *
 * @author metaforge
 */
@Service
public class GraphTraversalService {

    private static final Logger log = LoggerFactory.getLogger(GraphTraversalService.class);

    private final EntityDataPort entityDataPort;
    private final RelationDataPort relationDataPort;

    public GraphTraversalService(EntityDataPort entityDataPort, RelationDataPort relationDataPort) {
        this.entityDataPort = entityDataPort;
        this.relationDataPort = relationDataPort;
    }

    /**
     * 执行 BFS 遍历，返回在指定深度内发现的实体与关系集合。
     * <p>按层（level-order）扩展：每层先收集全部候选邻居 FQN，再批量取实体快照，
     * 避免逐实体 N+1 查询。过滤条件在遍历过程中实时生效。</p>
     */
    public void executeBfs(GraphQuery query) {
        Set<String> visited = new HashSet<>();
        boolean depthExceeded = false;

        EntitySnapshot startEntity = entityDataPort.findByFqn(query.getSourceFqn());
        if (startEntity == null) return;

        visited.add(startEntity.getFqn().getValue());
        query.collectEntity(startEntity);

        int effectiveGlobal = query.getTraversalDepth() != null
                ? Math.min(query.getMaxDepth(), query.getTraversalDepth().getGlobalMaxDepth())
                : query.getMaxDepth();

        List<BfsNode> currentLayer = new ArrayList<>();
        currentLayer.add(new BfsNode(startEntity, 0, query.getDirection()));

        int layer = 0;
        while (!currentLayer.isEmpty() && !query.isTimeout() && !query.isResultCountExceeded()) {
            layer++;
            if (layer > effectiveGlobal) {
                for (BfsNode node : currentLayer) {
                    if (!fetchRelations(node, query).isEmpty()) {
                        depthExceeded = true;
                    }
                }
                break;
            }

            // 1) 收集本层全部候选邻居 FQN 与关系（含方向/深度约束与过滤）
            List<FQN> nextFqns = new ArrayList<>();
            List<RelationSnapshot> passedRelations = new ArrayList<>();
            for (BfsNode node : currentLayer) {
                for (RelationSnapshot relation : fetchRelations(node, query)) {
                    if (!query.applyFilters(null, relation)) {
                        continue;
                    }
                    int relEffectiveDepth = query.getTraversalDepth() != null
                            ? query.getTraversalDepth().effectiveDepth(relation.getAssociationType())
                            : effectiveGlobal;
                    relEffectiveDepth = Math.min(effectiveGlobal, relEffectiveDepth);
                    if (node.depth + 1 > relEffectiveDepth) {
                        depthExceeded = true;
                        continue;
                    }

                    FQN nextFqn = node.entity.getFqn().equals(relation.getSourceEntityFqn())
                            ? relation.getTargetEntityFqn() : relation.getSourceEntityFqn();
                    if (nextFqn == null || visited.contains(nextFqn.getValue())) continue;

                    visited.add(nextFqn.getValue());
                    nextFqns.add(nextFqn);
                    passedRelations.add(relation);
                }
            }

            if (nextFqns.isEmpty()) {
                break;
            }

            // 2) 批量取本层所有邻居实体快照（替代逐实体 findByFqn，消除 N+1）
            Map<String, EntitySnapshot> nextEntityMap = new HashMap<>();
            for (EntitySnapshot snapshot : entityDataPort.batchFindByFqns(nextFqns)) {
                nextEntityMap.put(snapshot.getFqn().getValue(), snapshot);
            }

            // 3) 过滤实体 + 收集并入队下一层
            List<BfsNode> nextLayer = new ArrayList<>();
            for (int i = 0; i < nextFqns.size(); i++) {
                FQN nextFqn = nextFqns.get(i);
                EntitySnapshot nextEntity = nextEntityMap.get(nextFqn.getValue());
                if (nextEntity == null) {
                    visited.remove(nextFqn.getValue());
                    continue;
                }
                if (!query.applyFilters(nextEntity, null)) {
                    visited.remove(nextFqn.getValue());
                    continue;
                }

                query.collectRelation(passedRelations.get(i));
                query.collectEntity(nextEntity);
                nextLayer.add(new BfsNode(nextEntity, layer, query.getDirection()));
            }
            currentLayer = nextLayer;
        }

        if (query.isTimeout()) {
            query.markTruncated(TruncatedReason.TIMEOUT);
        } else if (query.isResultCountExceeded()) {
            query.markTruncated(TruncatedReason.COUNT_EXCEEDED);
        } else if (depthExceeded) {
            query.markTruncated(TruncatedReason.DEPTH_EXCEEDED);
        }
    }

    private List<RelationSnapshot> fetchRelations(BfsNode node, GraphQuery query) {
        Set<AssociationType> types = query.getRelationTypes();
        return switch (node.direction) {
            case FORWARD, DIRECTED -> relationDataPort.findOutboundRelations(node.entity.getFqn(), types, 100);
            case BACKWARD -> relationDataPort.findInboundRelations(node.entity.getFqn(), types, 100);
            case BIDIRECTIONAL -> {
                List<RelationSnapshot> all = new ArrayList<>();
                all.addAll(relationDataPort.findOutboundRelations(node.entity.getFqn(), types, 50));
                all.addAll(relationDataPort.findInboundRelations(node.entity.getFqn(), types, 50));
                yield all;
            }
        };
    }

    /**
     * 执行 DFS 遍历，收集所有路径。
     * 用于路径枚举场景。
     */
    public List<TraversalPath> executeDfs(GraphQuery query, FQN targetFqn) {
        List<TraversalPath> allPaths = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        EntitySnapshot startEntity = entityDataPort.findByFqn(query.getSourceFqn());
        if (startEntity == null) return allPaths;

        visited.add(startEntity.getFqn().getValue());
        dfs(query, startEntity, targetFqn, visited, new ArrayList<>(), new ArrayList<>(), 0, allPaths);
        return allPaths;
    }

    private void dfs(GraphQuery query, EntitySnapshot current, FQN targetFqn,
                     Set<String> visited, List<PathSegmentVO> currentPath,
                     List<RelationSnapshot> currentRels, int depth,
                     List<TraversalPath> allPaths) {
        if (query.isTimeout() || depth >= query.getMaxDepth()) return;
        if (allPaths.size() >= query.getMaxResultCount()) return;

        if (current.getFqn().equals(targetFqn) && !currentPath.isEmpty()) {
            TraversalPath path = new TraversalPath();
            for (PathSegmentVO seg : currentPath) {
                path.addSegment(seg);
            }
            allPaths.add(path);
            return;
        }

        // 1) 收集当前节点所有出边（含过滤），未访问的邻居批量取快照
        Set<AssociationType> types = query.getRelationTypes();
        List<RelationSnapshot> outRels = relationDataPort.findOutboundRelations(current.getFqn(), types, 100);

        List<FQN> candidateFqns = new ArrayList<>();
        List<RelationSnapshot> candidateRels = new ArrayList<>();
        for (RelationSnapshot relation : outRels) {
            if (!query.applyFilters(null, relation)) {
                continue;
            }
            FQN nextFqn = relation.getTargetEntityFqn();
            if (nextFqn == null || visited.contains(nextFqn.getValue())) continue;
            candidateFqns.add(nextFqn);
            candidateRels.add(relation);
        }

        if (candidateFqns.isEmpty()) {
            return;
        }

        // 2) 批量取邻居实体快照
        Map<String, EntitySnapshot> nextEntityMap = new HashMap<>();
        for (EntitySnapshot snapshot : entityDataPort.batchFindByFqns(candidateFqns)) {
            nextEntityMap.put(snapshot.getFqn().getValue(), snapshot);
        }

        // 3) 对每个邻居递归扩展
        for (int i = 0; i < candidateFqns.size(); i++) {
            FQN nextFqn = candidateFqns.get(i);
            EntitySnapshot nextEntity = nextEntityMap.get(nextFqn.getValue());
            if (nextEntity == null || !query.applyFilters(nextEntity, null)) {
                continue;
            }

            visited.add(nextFqn.getValue());
            RelationSnapshot relation = candidateRels.get(i);

            PathSegmentVO segment = new PathSegmentVO(
                    current.getFqn(), nextFqn, relation.getFqn(),
                    relation.getAssociationType(), TraversalDirection.FORWARD, 1.0);
            currentPath.add(segment);
            currentRels.add(relation);

            dfs(query, nextEntity, targetFqn, visited, currentPath, currentRels, depth + 1, allPaths);

            currentPath.remove(currentPath.size() - 1);
            currentRels.remove(currentRels.size() - 1);
            visited.remove(nextFqn.getValue());
        }
    }

    /**
     * 计算指定深度的邻接实体集合。
     */
    public void computeAdjacency(GraphQuery query, int depth) {
        executeBfs(query);
        query.computeAdjacency();
    }

    private static class BfsNode {
        final EntitySnapshot entity;
        final int depth;
        final TraversalDirection direction;

        BfsNode(EntitySnapshot entity, int depth) {
            this(entity, depth, TraversalDirection.FORWARD);
        }

        BfsNode(EntitySnapshot entity, int depth, TraversalDirection direction) {
            this.entity = entity;
            this.depth = depth;
            this.direction = direction;
        }
    }
}
