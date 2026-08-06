package com.metaforge.agent.cognition.core.application.executor.impl;

import com.metaforge.agent.cognition.api.enums.PerspectiveCode;
import com.metaforge.agent.cognition.api.perspective.PerspectiveExecutionContext;
import com.metaforge.agent.cognition.api.perspective.PerspectiveExecutor;
import com.metaforge.agent.cognition.core.domain.model.entity.CompositionTree;
import com.metaforge.computeengine.api.dto.common.RelationSummary;
import com.metaforge.computeengine.api.dto.response.GraphQueryResult;
import com.metaforge.metadata.api.dto.response.MetadataEntityDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CompositionTreeExecutor implements PerspectiveExecutor {

    private static final Logger log = LoggerFactory.getLogger(CompositionTreeExecutor.class);

    private final ExecutorSupport support;

    public CompositionTreeExecutor(ExecutorSupport support) {
        this.support = support;
    }

    @Override
    public PerspectiveCode supportedPerspective() {
        return PerspectiveCode.COMPOSITION_TREE;
    }

    @Override
    public Object execute(PerspectiveExecutionContext ctx) {
        log.debug("执行组成结构视角(compute-engine): entityFqn={}", ctx.entityFqn());

        CompositionTree tree = new CompositionTree();
        tree.setRootFqn(ctx.entityFqn());
        tree.setDirection("BOTH");
        tree.setTruncated(false);

        if (ctx.entityFqn() == null) {
            tree.setRoot(buildNode(ctx.entityFqn(), 0));
            return tree;
        }

        GraphQueryResult result = support.compositionTree(ctx.entityFqn(), "BOTH", 4);
        if (result.truncated()) {
            tree.setTruncated(true);
        }

        Map<String, List<String>> childrenMap = new LinkedHashMap<>();
        Map<String, List<String>> parentMap = new LinkedHashMap<>();
        for (RelationSummary relation : support.relationsOf(result)) {
            if (relation.sourceEntityFqn() == null || relation.targetEntityFqn() == null) {
                continue;
            }
            childrenMap.computeIfAbsent(relation.sourceEntityFqn(), k -> new ArrayList<>())
                    .add(relation.targetEntityFqn());
            parentMap.computeIfAbsent(relation.targetEntityFqn(), k -> new ArrayList<>())
                    .add(relation.sourceEntityFqn());
        }

        CompositionTree.TreeNode root = buildNode(ctx.entityFqn(), 0);
        tree.setRoot(root);

        Map<String, Integer> visited = new HashMap<>();
        visited.put(ctx.entityFqn(), 0);
        buildChildren(root, childrenMap, 1, visited);
        buildParents(root, parentMap, 1, visited);

        return tree;
    }

    private void buildChildren(CompositionTree.TreeNode node, Map<String, List<String>> childrenMap,
                               int depth, Map<String, Integer> visited) {
        if (depth > 4) {
            node.setChildren(new ArrayList<>());
            return;
        }
        List<CompositionTree.TreeNode> children = new ArrayList<>();
        List<String> childFqns = childrenMap.getOrDefault(node.getFqn(), List.of());
        for (String childFqn : childFqns) {
            if (visited.containsKey(childFqn)) {
                continue;
            }
            visited.put(childFqn, depth);
            CompositionTree.TreeNode child = buildNode(childFqn, depth);
            children.add(child);
            buildChildren(child, childrenMap, depth + 1, visited);
        }
        node.setChildren(children);
    }

    private void buildParents(CompositionTree.TreeNode node, Map<String, List<String>> parentMap,
                              int depth, Map<String, Integer> visited) {
        if (depth > 4) {
            return;
        }
        List<CompositionTree.TreeNode> parents = new ArrayList<>();
        List<String> parentFqns = parentMap.getOrDefault(node.getFqn(), List.of());
        for (String parentFqn : parentFqns) {
            if (visited.containsKey(parentFqn)) {
                continue;
            }
            visited.put(parentFqn, depth);
            CompositionTree.TreeNode parent = buildNode(parentFqn, depth);
            parents.add(parent);
            buildChildren(parent, parentMap, depth + 1, visited);
        }
        node.getChildren().addAll(parents);
    }

    private CompositionTree.TreeNode buildNode(String fqn, int depth) {
        CompositionTree.TreeNode node = new CompositionTree.TreeNode();
        node.setFqn(fqn);
        node.setName(extractName(fqn));
        node.setEntitySchemaFqn(resolveSchemaFqn(fqn));
        node.setDepth(depth);
        node.setChildren(new ArrayList<>());
        node.setParentChain(new ArrayList<>());
        return node;
    }

    private String resolveSchemaFqn(String fqn) {
        if (fqn == null) return null;
        Object raw = support.metadata().getByFqn(fqn);
        if (raw instanceof MetadataEntityDto entity) {
            return entity.getEntitySchemaFqn();
        }
        return null;
    }

    private String extractName(String fqn) {
        if (fqn == null) return "";
        String[] parts = fqn.split("\\.");
        return parts.length > 0 ? parts[parts.length - 1] : fqn;
    }
}
