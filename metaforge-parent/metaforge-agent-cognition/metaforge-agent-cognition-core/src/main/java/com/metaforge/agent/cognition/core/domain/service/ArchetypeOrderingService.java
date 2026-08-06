package com.metaforge.agent.cognition.core.domain.service;

import com.metaforge.agent.cognition.api.enums.AgentArchetype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ArchetypeOrderingService {

    private static final Logger log = LoggerFactory.getLogger(ArchetypeOrderingService.class);

    private static final List<String> EXECUTION_ORDER = List.of(
            "constraint_set", "capability_catalog", "flow_blueprint", "entity_profile",
            "decision_matrix", "impact_trace", "prerequisite_chain", "composition_tree",
            "relationship_graph", "domain_location", "domain_navigation", "instance_catalog",
            "bundle_directory", "schema_inventory");

    private static final List<String> EXPLORATION_ORDER = List.of(
            "composition_tree", "relationship_graph", "domain_location", "domain_navigation",
            "entity_profile", "instance_catalog", "schema_inventory", "capability_catalog",
            "flow_blueprint", "decision_matrix", "impact_trace", "prerequisite_chain",
            "constraint_set", "bundle_directory");

    private static final List<String> AUDIT_ORDER = List.of(
            "constraint_set", "impact_trace", "prerequisite_chain", "entity_profile",
            "decision_matrix", "composition_tree", "relationship_graph", "capability_catalog",
            "flow_blueprint", "domain_location", "domain_navigation", "instance_catalog",
            "schema_inventory", "bundle_directory");

    private static final List<String> ORCHESTRATION_ORDER = List.of(
            "flow_blueprint", "decision_matrix", "capability_catalog", "entity_profile",
            "prerequisite_chain", "constraint_set", "impact_trace", "composition_tree",
            "relationship_graph", "domain_location", "domain_navigation", "instance_catalog",
            "schema_inventory", "bundle_directory");

    public List<String> orderPerspectives(List<String> perspectiveIds, AgentArchetype archetype) {
        if (perspectiveIds == null || perspectiveIds.isEmpty()) {
            return perspectiveIds;
        }

        List<String> priorityOrder = getPriorityOrder(archetype);
        List<String> ordered = new ArrayList<>(perspectiveIds);

        ordered.sort(Comparator.comparingInt(id -> {
            int idx = priorityOrder.indexOf(id);
            return idx == -1 ? Integer.MAX_VALUE : idx;
        }));

        log.debug("代理原型排序: archetype={}, before={}, after={}", archetype, perspectiveIds, ordered);
        return ordered;
    }

    private List<String> getPriorityOrder(AgentArchetype archetype) {
        if (archetype == null) {
            log.warn("代理原型为空，回退默认 execution");
            return EXECUTION_ORDER;
        }
        return switch (archetype) {
            case EXECUTION -> EXECUTION_ORDER;
            case EXPLORATION -> EXPLORATION_ORDER;
            case AUDIT -> AUDIT_ORDER;
            case ORCHESTRATION -> ORCHESTRATION_ORDER;
        };
    }
}
