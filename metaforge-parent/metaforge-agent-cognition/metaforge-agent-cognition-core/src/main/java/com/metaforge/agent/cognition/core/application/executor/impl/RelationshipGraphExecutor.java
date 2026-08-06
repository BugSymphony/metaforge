package com.metaforge.agent.cognition.core.application.executor.impl;

import com.metaforge.agent.cognition.api.enums.PerspectiveCode;
import com.metaforge.agent.cognition.api.perspective.PerspectiveExecutionContext;
import com.metaforge.agent.cognition.api.perspective.PerspectiveExecutor;
import com.metaforge.agent.cognition.core.domain.model.entity.RelationshipGraph;
import com.metaforge.computeengine.api.dto.common.RelationSummary;
import com.metaforge.computeengine.api.dto.response.GraphQueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RelationshipGraphExecutor implements PerspectiveExecutor {

    private static final Logger log = LoggerFactory.getLogger(RelationshipGraphExecutor.class);

    private final ExecutorSupport support;

    public RelationshipGraphExecutor(ExecutorSupport support) {
        this.support = support;
    }

    @Override
    public PerspectiveCode supportedPerspective() {
        return PerspectiveCode.RELATIONSHIP_GRAPH;
    }

    @Override
    public Object execute(PerspectiveExecutionContext ctx) {
        log.debug("执行关系图谱视角(compute-engine): entityFqn={}", ctx.entityFqn());

        RelationshipGraph graph = new RelationshipGraph();
        graph.setCenterFqn(ctx.entityFqn());
        graph.setDegrees(3);
        graph.setGroups(new LinkedHashMap<>());

        if (ctx.entityFqn() == null) {
            graph.setEmpty(true);
            graph.setEmptyNote("未指定实体 FQN，无法查询关系图谱");
            return graph;
        }

        Map<String, RelationshipGraph.RelationGroup> groups = new LinkedHashMap<>();
        java.util.Set<String> seen = new java.util.HashSet<>();

        GraphQueryResult outResult = support.adjacency(ctx.entityFqn(), "FORWARD", 1, null);
        for (RelationSummary relation : support.relationsOf(outResult)) {
            if (relation.sourceEntityFqn() == null || !seen.add(relation.fqn())) {
                continue;
            }
            addRelation(groups, relation);
        }

        GraphQueryResult inResult = support.adjacency(ctx.entityFqn(), "BACKWARD", 1, null);
        for (RelationSummary relation : support.relationsOf(inResult)) {
            if (relation.targetEntityFqn() == null || !seen.add(relation.fqn())) {
                continue;
            }
            addRelation(groups, relation);
        }

        graph.setGroups(groups);
        graph.setEmpty(groups.isEmpty());
        if (groups.isEmpty()) {
            graph.setEmptyNote("该实体暂无关系数据");
        }
        return graph;
    }

    private void addRelation(Map<String, RelationshipGraph.RelationGroup> groups,
                             RelationSummary relation) {
        String type = relation.associationType() != null ? relation.associationType().name() : "RELATION";
        RelationshipGraph.RelationGroup group = groups.computeIfAbsent(type, k -> {
            RelationshipGraph.RelationGroup g = new RelationshipGraph.RelationGroup();
            g.setAssociationType(k);
            g.setRelations(new ArrayList<>());
            return g;
        });

        RelationshipGraph.RelationGroup.RelationDetail detail =
                new RelationshipGraph.RelationGroup.RelationDetail();
        detail.setRelationFqn(relation.fqn());
        detail.setSourceEntityFqn(relation.sourceEntityFqn());
        detail.setTargetEntityFqn(relation.targetEntityFqn());
        detail.setSemanticDescription(relation.sourceEntityFqn() + " -> " + relation.targetEntityFqn());
        group.getRelations().add(detail);
    }
}
