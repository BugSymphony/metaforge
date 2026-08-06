package com.metaforge.metamodel.domain.service;

import com.metaforge.metamodel.domain.exception.CircularDependencyException;
import com.metaforge.metamodel.domain.repository.BundleDependencyRepository;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 循环依赖检测器。
 * 基于 Kahn 拓扑排序 + DFS 组合检测，发现环时报告完整环路径。
 */
@Component
public class CircularDependencyDetector {

    /**
     * 检测依赖图中是否存在循环依赖。
     * 如果存在环，抛出 CircularDependencyException 并包含完整环路径。
     */
    public void detectCycles(BundleDependencyRepository dependencyRepository) {
        List<String> allSources = dependencyRepository.findAllSourceFqns();
        if (allSources.isEmpty()) {
            return;
        }

        // 构建图，收集所有节点
        Map<String, List<String>> graph = new LinkedHashMap<>();
        Set<String> allNodes = new LinkedHashSet<>(allSources);

        for (String source : allSources) {
            List<String> targets = dependencyRepository.findTargetFqnsBySource(source);
            graph.put(source, targets);
            allNodes.addAll(targets);
        }

        // 确保所有节点都在图中
        for (String node : allNodes) {
            graph.putIfAbsent(node, Collections.emptyList());
        }

        // Kahn 拓扑排序
        Map<String, Integer> inDegree = new LinkedHashMap<>();
        for (String node : allNodes) {
            inDegree.put(node, 0);
        }
        for (var entry : graph.entrySet()) {
            for (String target : entry.getValue()) {
                inDegree.merge(target, 1, Integer::sum);
            }
        }

        Deque<String> queue = new ArrayDeque<>();
        for (var entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        int visited = 0;
        while (!queue.isEmpty()) {
            String node = queue.poll();
            visited++;
            for (String neighbor : graph.getOrDefault(node, Collections.emptyList())) {
                inDegree.merge(neighbor, -1, Integer::sum);
                if (inDegree.get(neighbor) == 0) {
                    queue.add(neighbor);
                }
            }
        }

        if (visited < allNodes.size()) {
            List<List<String>> cycles = findCycles(graph, allNodes, inDegree);
            throw new CircularDependencyException(cycles);
        }
    }

    /**
     * 检测添加新依赖后是否会形成环。
     */
    public void validateNewDependency(BundleDependencyRepository dependencyRepository,
                                       String sourceFqn, String targetFqn) {
        // 检查直接自引用
        if (sourceFqn.equals(targetFqn)) {
            throw new CircularDependencyException(
                    List.of(List.of(sourceFqn, targetFqn)));
        }

        // 检查目标是否已直接或间接依赖于源（BFS 从 target 出发找 source）
        Set<String> visited = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(targetFqn);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (current.equals(sourceFqn)) {
                throw new CircularDependencyException(
                        List.of(List.of(sourceFqn, targetFqn, sourceFqn)));
            }
            if (!visited.add(current)) continue;
            List<String> deps = dependencyRepository.findTargetFqnsBySource(current);
            for (String dep : deps) {
                if (!visited.contains(dep)) {
                    queue.add(dep);
                }
            }
        }
    }

    private List<List<String>> findCycles(Map<String, List<String>> graph,
                                           Set<String> nodes,
                                           Map<String, Integer> remainingInDegree) {
        List<List<String>> cycles = new ArrayList<>();
        Set<String> inCycle = new LinkedHashSet<>();

        for (String node : nodes) {
            if (remainingInDegree.getOrDefault(node, 0) > 0 && !inCycle.contains(node)) {
                List<String> path = new ArrayList<>();
                Set<String> pathSet = new LinkedHashSet<>();
                dfsFindCycle(node, graph, path, pathSet, cycles, inCycle);
            }
        }
        return cycles.isEmpty() ? cycles : cycles;
    }

    private boolean dfsFindCycle(String current, Map<String, List<String>> graph,
                                  List<String> path, Set<String> pathSet,
                                  List<List<String>> cycles, Set<String> inCycle) {
        if (pathSet.contains(current)) {
            int startIdx = path.indexOf(current);
            if (startIdx >= 0) {
                List<String> cycle = new ArrayList<>(path.subList(startIdx, path.size()));
                cycle.add(current);
                cycles.add(cycle);
                inCycle.addAll(cycle);
            }
            return true;
        }

        path.add(current);
        pathSet.add(current);

        for (String neighbor : graph.getOrDefault(current, Collections.emptyList())) {
            if (dfsFindCycle(neighbor, graph, path, pathSet, cycles, inCycle)) {
                break;
            }
        }

        path.remove(path.size() - 1);
        pathSet.remove(current);
        return false;
    }
}
