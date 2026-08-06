package com.metaforge.computeengine.domain.service;

import com.metaforge.computeengine.api.enums.AssociationType;
import com.metaforge.computeengine.api.enums.TraversalDirection;
import com.metaforge.computeengine.domain.model.aggregate.ImpactQuery;
import com.metaforge.computeengine.domain.model.entity.ImpactEntity;
import com.metaforge.computeengine.domain.model.entity.TraversalPath;
import com.metaforge.computeengine.domain.model.valueobject.FQN;
import com.metaforge.computeengine.domain.model.valueobject.PathSegmentVO;
import com.metaforge.computeengine.domain.model.valueobject.RelationSnapshot;
import com.metaforge.computeengine.domain.port.EntityDataPort;
import com.metaforge.computeengine.domain.port.RelationDataPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * 影响分析领域服务。
 *
 * <p>提供三种影响分析算法：
 * <ul>
 *   <li>diffuse — 正向影响扩散（沿出边 BFS）</li>
 *   <li>trace — 反向依赖溯源（沿入边 BFS）</li>
 *   <li>impactPaths — 两点间影响传导路径枚举（DFS）</li>
 * </ul>
 *
 * @author metaforge
 */
@Service
public class ImpactAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ImpactAnalysisService.class);

    private final EntityDataPort entityDataPort;
    private final RelationDataPort relationDataPort;

    public ImpactAnalysisService(EntityDataPort entityDataPort, RelationDataPort relationDataPort) {
        this.entityDataPort = entityDataPort;
        this.relationDataPort = relationDataPort;
    }

    /**
     * 正向影响扩散——沿出边 BFS 扩展，按层级分组。
     */
    public void diffuse(ImpactQuery query) {
        Set<String> visited = new HashSet<>();
        Queue<DiffuseNode> queue = new LinkedList<>();

        var startEntity = entityDataPort.findByFqn(query.getCenterFqn());
        if (startEntity == null) return;

        visited.add(startEntity.getFqn().getValue());
        query.addImpactEntity(0, new ImpactEntity(startEntity.getFqn(), 0, new HashSet<>()));
        queue.add(new DiffuseNode(startEntity.getFqn(), 0));

        while (!queue.isEmpty()) {
            DiffuseNode node = queue.poll();
            if (query.isTimeout() || node.depth >= query.getMaxDepth()) continue;

            Set<AssociationType> types = query.getRelationTypes();
            List<RelationSnapshot> relations = relationDataPort.findOutboundRelations(
                    node.fqn, types, 500);

            for (RelationSnapshot rel : relations) {
                FQN nextFqn = rel.getTargetEntityFqn();
                if (nextFqn == null || visited.contains(nextFqn.getValue())) continue;

                visited.add(nextFqn.getValue());
                Set<AssociationType> affectedTypes = new LinkedHashSet<>();
                affectedTypes.add(rel.getAssociationType());
                query.addImpactEntity(node.depth + 1,
                        new ImpactEntity(nextFqn, node.depth + 1, affectedTypes));
                queue.add(new DiffuseNode(nextFqn, node.depth + 1));
            }
        }
    }

    /**
     * 反向依赖溯源——沿入边 BFS 追溯，按层级分组。
     */
    public void trace(ImpactQuery query) {
        Set<String> visited = new HashSet<>();
        Queue<DiffuseNode> queue = new LinkedList<>();

        var startEntity = entityDataPort.findByFqn(query.getCenterFqn());
        if (startEntity == null) return;

        visited.add(startEntity.getFqn().getValue());
        query.addImpactEntity(0, new ImpactEntity(startEntity.getFqn(), 0, new HashSet<>()));
        queue.add(new DiffuseNode(startEntity.getFqn(), 0));

        while (!queue.isEmpty()) {
            DiffuseNode node = queue.poll();
            if (query.isTimeout() || node.depth >= query.getMaxDepth()) continue;

            Set<AssociationType> types = query.getRelationTypes();
            List<RelationSnapshot> relations = relationDataPort.findInboundRelations(
                    node.fqn, types, 500);

            for (RelationSnapshot rel : relations) {
                FQN nextFqn = rel.getSourceEntityFqn();
                if (nextFqn == null || visited.contains(nextFqn.getValue())) continue;

                visited.add(nextFqn.getValue());
                Set<AssociationType> affectedTypes = new LinkedHashSet<>();
                affectedTypes.add(rel.getAssociationType());
                query.addImpactEntity(node.depth + 1,
                        new ImpactEntity(nextFqn, node.depth + 1, affectedTypes));
                queue.add(new DiffuseNode(nextFqn, node.depth + 1));
            }
        }
    }

    /**
     * 两点间影响传导路径枚举。
     */
    public List<TraversalPath> impactPaths(FQN sourceFqn, FQN targetFqn,
                                            Set<AssociationType> types, int maxDepth,
                                            long timeoutMs) {
        List<TraversalPath> allPaths = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        long startTime = System.currentTimeMillis();

        var startEntity = entityDataPort.findByFqn(sourceFqn);
        if (startEntity == null) return allPaths;

        visited.add(sourceFqn.getValue());
        dfsImpact(sourceFqn, targetFqn, types, maxDepth, visited, new ArrayList<>(),
                allPaths, timeoutMs, startTime);
        return allPaths;
    }

    private void dfsImpact(FQN current, FQN target, Set<AssociationType> types,
                           int maxDepth, Set<String> visited,
                           List<PathSegmentVO> currentPath,
                           List<TraversalPath> allPaths,
                           long timeoutMs, long startTime) {
        if (System.currentTimeMillis() - startTime > timeoutMs) return;
        int depth = currentPath.size();
        if (depth >= maxDepth) return;

        if (current.equals(target) && !currentPath.isEmpty()) {
            TraversalPath path = new TraversalPath();
            for (PathSegmentVO seg : currentPath) {
                path.addSegment(seg);
            }
            allPaths.add(path);
            return;
        }

        List<RelationSnapshot> relations = relationDataPort.findRelations(
                current, TraversalDirection.BIDIRECTIONAL, types, 100);

        for (RelationSnapshot rel : relations) {
            FQN next = null;
            if (rel.getSourceEntityFqn() != null && rel.getSourceEntityFqn().equals(current)) {
                next = rel.getTargetEntityFqn();
            } else if (rel.getTargetEntityFqn() != null && rel.getTargetEntityFqn().equals(current)) {
                next = rel.getSourceEntityFqn();
            }
            if (next == null || visited.contains(next.getValue())) continue;

            visited.add(next.getValue());
            currentPath.add(new PathSegmentVO(current, next, rel.getFqn(),
                    rel.getAssociationType(), TraversalDirection.FORWARD, 1.0));

            dfsImpact(next, target, types, maxDepth, visited, currentPath,
                    allPaths, timeoutMs, startTime);

            visited.remove(next.getValue());
            currentPath.remove(currentPath.size() - 1);
        }
    }

    private record DiffuseNode(FQN fqn, int depth) {}
}
