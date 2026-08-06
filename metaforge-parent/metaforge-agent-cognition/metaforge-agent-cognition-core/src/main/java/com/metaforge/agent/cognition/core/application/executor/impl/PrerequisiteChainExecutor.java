package com.metaforge.agent.cognition.core.application.executor.impl;

import com.metaforge.agent.cognition.api.enums.PerspectiveCode;
import com.metaforge.agent.cognition.api.perspective.PerspectiveExecutionContext;
import com.metaforge.agent.cognition.api.perspective.PerspectiveExecutor;
import com.metaforge.agent.cognition.core.domain.model.entity.PrerequisiteChain;
import com.metaforge.computeengine.api.dto.common.RelationSummary;
import com.metaforge.computeengine.api.dto.response.GraphQueryResult;
import com.metaforge.metadata.api.dto.response.MetadataEntityDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PrerequisiteChainExecutor implements PerspectiveExecutor {

    private static final Logger log = LoggerFactory.getLogger(PrerequisiteChainExecutor.class);

    private final ExecutorSupport support;

    public PrerequisiteChainExecutor(ExecutorSupport support) {
        this.support = support;
    }

    @Override
    public PerspectiveCode supportedPerspective() {
        return PerspectiveCode.PREREQUISITE_CHAIN;
    }

    @Override
    public Object execute(PerspectiveExecutionContext ctx) {
        log.debug("执行前置依赖视角(compute-engine): entityFqn={}", ctx.entityFqn());

        PrerequisiteChain chain = new PrerequisiteChain();
        chain.setEntityFqn(ctx.entityFqn());
        chain.setDependencyTree(new ArrayList<>());

        if (ctx.entityFqn() == null) {
            return chain;
        }

        Map<String, List<RelationSummary>> outboundBySource = new HashMap<>();
        GraphQueryResult result;
        try {
            result = support.adjacency(ctx.entityFqn(), "FORWARD", 3,
                    List.of("DEPENDENCY_INFLUENCE"));
        } catch (Exception e) {
            log.error("前置依赖视角 adjacency 查询异常: entityFqn={}, error={}", ctx.entityFqn(), e.toString(), e);
            chain.setDependencyTree(new ArrayList<>());
            return chain;
        }
        for (RelationSummary relation : support.relationsOf(result)) {
            if (relation.sourceEntityFqn() == null || relation.targetEntityFqn() == null) {
                continue;
            }
            outboundBySource.computeIfAbsent(relation.sourceEntityFqn(), k -> new ArrayList<>())
                    .add(relation);
        }

        List<PrerequisiteChain.PrerequisiteNode> tree = new ArrayList<>();
        Map<String, Integer> visited = new HashMap<>();
        buildNodes(ctx.entityFqn(), tree, 0, visited, outboundBySource);
        chain.setDependencyTree(tree);
        return chain;
    }

    private void buildNodes(String entityFqn, List<PrerequisiteChain.PrerequisiteNode> nodes,
                            int level, Map<String, Integer> visited,
                            Map<String, List<RelationSummary>> outboundBySource) {
        if (level >= 3 || visited.containsKey(entityFqn)) {
            return;
        }
        visited.put(entityFqn, level);

        List<RelationSummary> relations = outboundBySource.getOrDefault(entityFqn, List.of());
        for (RelationSummary relation : relations) {
            String depFqn = relation.targetEntityFqn();
            PrerequisiteChain.PrerequisiteNode node = new PrerequisiteChain.PrerequisiteNode();
            node.setEntityFqn(depFqn);
            node.setEntityName(resolveName(depFqn));
            node.setDependencyType(relation.associationType() != null
                    ? relation.associationType().name() : "DEPENDENCY");
            node.setBlocking(false);
            node.setEntityStatus(resolveStatus(depFqn));
            node.setLevel(level + 1);
            node.setChildren(new ArrayList<>());
            buildNodes(depFqn, node.getChildren(), level + 1, visited, outboundBySource);
            nodes.add(node);
        }
    }

    private String resolveName(String fqn) {
        if (fqn == null) return "";
        Object raw = support.metadata().getByFqn(fqn);
        if (raw instanceof MetadataEntityDto entity && entity.getName() != null) {
            return entity.getName();
        }
        String[] parts = fqn.split("\\.");
        return parts.length > 0 ? parts[parts.length - 1] : fqn;
    }

    private String resolveStatus(String fqn) {
        if (fqn == null) return "UNKNOWN";
        Object raw = support.metadata().getByFqn(fqn);
        if (raw instanceof MetadataEntityDto entity && entity.getStatus() != null) {
            return entity.getStatus();
        }
        return "UNKNOWN";
    }
}
