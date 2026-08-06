package com.metaforge.computeengine.domain.service;

import com.metaforge.computeengine.api.enums.AssociationType;
import com.metaforge.computeengine.api.enums.TraversalDirection;
import com.metaforge.computeengine.domain.model.aggregate.PathQuery;
import com.metaforge.computeengine.domain.model.entity.ClosuredEntity;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * 路径推理引擎领域服务。
 *
 * <p>提供四种推理算法实现：
 * <ul>
 *   <li>findAllPaths — 递归 DFS 枚举两点间所有路径</li>
 *   <li>findShortestPath — BFS 最早到达即最短</li>
 *   <li>computeClosure — BFS 沿可传递关系类型扩展，按层级分组</li>
 *   <li>multiHopTraverse — 按跃步序列迭代 BFS</li>
 *   <li>checkReachability — LIMIT 1 早期终止</li>
 * </ul>
 *
 * @author metaforge
 */
@Service
public class PathInferenceService {

    private static final Logger log = LoggerFactory.getLogger(PathInferenceService.class);

    private final EntityDataPort entityDataPort;
    private final RelationDataPort relationDataPort;

    public PathInferenceService(EntityDataPort entityDataPort, RelationDataPort relationDataPort) {
        this.entityDataPort = entityDataPort;
        this.relationDataPort = relationDataPort;
    }

    /**
     * 两点间所有路径搜索（DFS）。
     */
    public List<TraversalPath> findAllPaths(PathQuery query) {
        List<TraversalPath> allPaths = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        visited.add(query.getSourceFqn().getValue());

        dfs(query.getSourceFqn(), query.getTargetFqn(), query, visited, new ArrayList<>(), 0, allPaths);
        return allPaths;
    }

    private void dfs(FQN current, FQN target, PathQuery query, Set<String> visited,
                     List<PathSegmentVO> currentPath, int depth, List<TraversalPath> allPaths) {
        if (query.isTimeout() || depth >= query.getMaxDepth()) return;

        if (current.equals(target) && !currentPath.isEmpty()) {
            TraversalPath path = new TraversalPath();
            for (PathSegmentVO seg : currentPath) {
                path.addSegment(seg);
            }
            allPaths.add(path);
            return;
        }

        Set<AssociationType> types = query.getRelationTypes();
        List<RelationSnapshot> outRels = relationDataPort.findOutboundRelations(current, types, 100);
        List<RelationSnapshot> inRels = relationDataPort.findInboundRelations(current, types, 100);

        List<RelationSnapshot> allRels = new ArrayList<>(outRels);
        allRels.addAll(inRels);

        for (RelationSnapshot relation : allRels) {
            FQN nextFqn = null;
            TraversalDirection dir = TraversalDirection.FORWARD;
            if (relation.getSourceEntityFqn() != null && relation.getSourceEntityFqn().equals(current)) {
                nextFqn = relation.getTargetEntityFqn();
                dir = TraversalDirection.FORWARD;
            } else if (relation.getTargetEntityFqn() != null && relation.getTargetEntityFqn().equals(current)) {
                nextFqn = relation.getSourceEntityFqn();
                dir = TraversalDirection.BACKWARD;
            }
            if (nextFqn == null || visited.contains(nextFqn.getValue())) continue;

            PathSegmentVO segment = new PathSegmentVO(
                    current, nextFqn, relation.getFqn(),
                    relation.getAssociationType(), dir, 1.0);
            currentPath.add(segment);
            visited.add(nextFqn.getValue());

            dfs(nextFqn, target, query, visited, currentPath, depth + 1, allPaths);

            visited.remove(nextFqn.getValue());
            currentPath.remove(currentPath.size() - 1);
        }
    }

    /**
     * 最短路径搜索（BFS）。
     */
    public TraversalPath findShortestPath(PathQuery query) {
        Queue<SearchNode> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        visited.add(query.getSourceFqn().getValue());
        List<PathSegmentVO> emptyPath = new ArrayList<>();
        queue.add(new SearchNode(query.getSourceFqn(), emptyPath, 0));

        while (!queue.isEmpty()) {
            SearchNode node = queue.poll();
            if (query.isTimeout() || node.depth >= query.getMaxDepth()) continue;

            if (node.fqn.equals(query.getTargetFqn()) && !node.path.isEmpty()) {
                TraversalPath result = new TraversalPath();
                for (PathSegmentVO seg : node.path) {
                    result.addSegment(seg);
                }
                return result;
            }

            Set<AssociationType> types = query.getRelationTypes();
            List<RelationSnapshot> relations = relationDataPort.findRelations(
                    node.fqn, TraversalDirection.BIDIRECTIONAL, types, 100);

            for (RelationSnapshot relation : relations) {
                FQN nextFqn = null;
                if (relation.getSourceEntityFqn() != null && relation.getSourceEntityFqn().equals(node.fqn)) {
                    nextFqn = relation.getTargetEntityFqn();
                } else if (relation.getTargetEntityFqn() != null && relation.getTargetEntityFqn().equals(node.fqn)) {
                    nextFqn = relation.getSourceEntityFqn();
                }
                if (nextFqn == null || visited.contains(nextFqn.getValue())) continue;

                visited.add(nextFqn.getValue());
                List<PathSegmentVO> newPath = new ArrayList<>(node.path);
                newPath.add(new PathSegmentVO(node.fqn, nextFqn, relation.getFqn(),
                        relation.getAssociationType(), TraversalDirection.FORWARD, 1.0));
                queue.add(new SearchNode(nextFqn, newPath, node.depth + 1));
            }
        }
        return null;
    }

    /**
     * 传递闭包计算（BFS 沿可传递关系扩展）。
     */
    public void computeClosure(PathQuery query, Set<AssociationType> transitiveTypes) {
        Queue<ClosureNode> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        EntitySnapshot startEntity = entityDataPort.findByFqn(query.getSourceFqn());
        if (startEntity == null) return;

        visited.add(startEntity.getFqn().getValue());
        query.addClosureEntity(0, new ClosuredEntity(startEntity.getFqn(), 0, new HashSet<>()));
        queue.add(new ClosureNode(startEntity.getFqn(), 0, new HashSet<>()));

        while (!queue.isEmpty()) {
            ClosureNode node = queue.poll();
            if (query.isTimeout() || node.depth >= query.getMaxDepth()) continue;

            for (AssociationType type : transitiveTypes) {
                int effectiveDepth = query.effectiveDepth(type);
                if (node.depth >= effectiveDepth) continue;

                List<RelationSnapshot> relations = relationDataPort.findRelations(
                        node.fqn, TraversalDirection.BIDIRECTIONAL,
                        Set.of(type), 100);

                for (RelationSnapshot rel : relations) {
                    FQN nextFqn = null;
                    if (rel.getSourceEntityFqn() != null && rel.getSourceEntityFqn().equals(node.fqn)) {
                        nextFqn = rel.getTargetEntityFqn();
                    } else if (rel.getTargetEntityFqn() != null && rel.getTargetEntityFqn().equals(node.fqn)) {
                        nextFqn = rel.getSourceEntityFqn();
                    }
                    if (nextFqn == null || visited.contains(nextFqn.getValue())) continue;

                    visited.add(nextFqn.getValue());
                    Set<AssociationType> arrivedTypes = new LinkedHashSet<>(node.arrivedByTypes);
                    arrivedTypes.add(type);

                    query.addClosureEntity(node.depth + 1,
                            new ClosuredEntity(nextFqn, node.depth + 1, arrivedTypes));
                    queue.add(new ClosureNode(nextFqn, node.depth + 1, arrivedTypes));
                }
            }
        }
    }

    /**
     * 多跳语义推理——按跃步序列逐层 BFS。
     */
    public List<TraversalPath> multiHopTraverse(PathQuery query, List<PathQuery.HopDefinition> hopDefs) {
        if (hopDefs == null || hopDefs.isEmpty()) return Collections.emptyList();
        if (hopDefs.size() > 3) {
            query.markTruncated(com.metaforge.computeengine.api.enums.TruncatedReason.DEPTH_EXCEEDED);
            return Collections.emptyList();
        }

        List<List<PathSegmentVO>> frontier = new ArrayList<>();
        frontier.add(new ArrayList<>());

        for (PathQuery.HopDefinition hop : hopDefs) {
            if (query.isTimeout()) {
                query.markTruncated(com.metaforge.computeengine.api.enums.TruncatedReason.TIMEOUT);
                return Collections.emptyList();
            }
            Set<AssociationType> types = Set.of(hop.relationType());
            List<List<PathSegmentVO>> next = new ArrayList<>();
            Set<String> seenEntities = new HashSet<>();
            for (List<PathSegmentVO> path : frontier) {
                FQN from = path.isEmpty() ? query.getSourceFqn() : path.get(path.size() - 1).getToEntity();
                List<RelationSnapshot> rels;
                if (hop.direction() == TraversalDirection.BACKWARD) {
                    rels = relationDataPort.findInboundRelations(from, types, 100);
                } else {
                    rels = relationDataPort.findOutboundRelations(from, types, 100);
                }
                for (RelationSnapshot rel : rels) {
                    FQN to = hop.direction() == TraversalDirection.BACKWARD
                            ? rel.getSourceEntityFqn()
                            : rel.getTargetEntityFqn();
                    if (to == null || seenEntities.contains(to.getValue())) continue;
                    seenEntities.add(to.getValue());
                    List<PathSegmentVO> newPath = new ArrayList<>(path);
                    newPath.add(new PathSegmentVO(from, to, rel.getFqn(), rel.getAssociationType(),
                            hop.direction(), 1.0));
                    next.add(newPath);
                }
            }
            if (next.isEmpty()) return Collections.emptyList();
            frontier = next;
        }

        List<TraversalPath> result = new ArrayList<>();
        for (List<PathSegmentVO> segments : frontier) {
            TraversalPath tp = new TraversalPath();
            segments.forEach(tp::addSegment);
            result.add(tp);
        }
        return result;
    }

    /**
     * 路径可达性快速判定（早期终止）。
     */
    public boolean checkReachability(PathQuery query) {
        Queue<FQN> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        visited.add(query.getSourceFqn().getValue());
        queue.add(query.getSourceFqn());

        while (!queue.isEmpty()) {
            FQN current = queue.poll();
            if (current.equals(query.getTargetFqn())) {
                query.setReachable(true);
                return true;
            }
            if (query.isTimeout()) return false;

            Set<AssociationType> types = query.getRelationTypes();
            List<RelationSnapshot> relations = relationDataPort.findRelations(
                    current, TraversalDirection.BIDIRECTIONAL, types, 100);

            for (RelationSnapshot rel : relations) {
                FQN next = null;
                if (rel.getSourceEntityFqn() != null && rel.getSourceEntityFqn().equals(current)) {
                    next = rel.getTargetEntityFqn();
                } else if (rel.getTargetEntityFqn() != null && rel.getTargetEntityFqn().equals(current)) {
                    next = rel.getSourceEntityFqn();
                }
                if (next != null && !visited.contains(next.getValue())) {
                    if (next.equals(query.getTargetFqn())) {
                        query.setReachable(true);
                        return true;
                    }
                    visited.add(next.getValue());
                    queue.add(next);
                }
            }
        }
        return false;
    }

    private record SearchNode(FQN fqn, List<PathSegmentVO> path, int depth) {}
    private record ClosureNode(FQN fqn, int depth, Set<AssociationType> arrivedByTypes) {}
}
