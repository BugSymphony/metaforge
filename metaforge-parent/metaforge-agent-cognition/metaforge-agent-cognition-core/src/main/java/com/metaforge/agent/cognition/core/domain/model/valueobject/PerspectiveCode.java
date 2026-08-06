package com.metaforge.agent.cognition.core.domain.model.valueobject;

import java.util.Set;

public record PerspectiveCode(String value) {

    private static final Set<String> VALID_CODES = Set.of(
            "entity_profile", "domain_location", "composition_tree", "relationship_graph",
            "constraint_set", "capability_catalog", "flow_blueprint", "decision_matrix",
            "impact_trace", "prerequisite_chain", "domain_navigation", "instance_catalog",
            "bundle_directory", "schema_inventory"
    );

    public static final PerspectiveCode ENTITY_PROFILE = new PerspectiveCode("entity_profile");
    public static final PerspectiveCode DOMAIN_LOCATION = new PerspectiveCode("domain_location");
    public static final PerspectiveCode COMPOSITION_TREE = new PerspectiveCode("composition_tree");
    public static final PerspectiveCode RELATIONSHIP_GRAPH = new PerspectiveCode("relationship_graph");
    public static final PerspectiveCode CONSTRAINT_SET = new PerspectiveCode("constraint_set");
    public static final PerspectiveCode CAPABILITY_CATALOG = new PerspectiveCode("capability_catalog");
    public static final PerspectiveCode FLOW_BLUEPRINT = new PerspectiveCode("flow_blueprint");
    public static final PerspectiveCode DECISION_MATRIX = new PerspectiveCode("decision_matrix");
    public static final PerspectiveCode IMPACT_TRACE = new PerspectiveCode("impact_trace");
    public static final PerspectiveCode PREREQUISITE_CHAIN = new PerspectiveCode("prerequisite_chain");
    public static final PerspectiveCode DOMAIN_NAVIGATION = new PerspectiveCode("domain_navigation");
    public static final PerspectiveCode INSTANCE_CATALOG = new PerspectiveCode("instance_catalog");
    public static final PerspectiveCode BUNDLE_DIRECTORY = new PerspectiveCode("bundle_directory");
    public static final PerspectiveCode SCHEMA_INVENTORY = new PerspectiveCode("schema_inventory");

    public PerspectiveCode {
        if (!VALID_CODES.contains(value)) {
            throw new IllegalArgumentException("Unknown perspective code: " + value);
        }
    }

    @Override
    public String toString() { return value; }
}
