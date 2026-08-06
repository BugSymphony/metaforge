package com.metaforge.agent.cognition.api.enums;

public enum PerspectiveCode {

    ENTITY_PROFILE("entity_profile"),
    DOMAIN_LOCATION("domain_location"),
    COMPOSITION_TREE("composition_tree"),
    RELATIONSHIP_GRAPH("relationship_graph"),
    CONSTRAINT_SET("constraint_set"),
    CAPABILITY_CATALOG("capability_catalog"),
    FLOW_BLUEPRINT("flow_blueprint"),
    DECISION_MATRIX("decision_matrix"),
    IMPACT_TRACE("impact_trace"),
    PREREQUISITE_CHAIN("prerequisite_chain"),
    DOMAIN_NAVIGATION("domain_navigation"),
    INSTANCE_CATALOG("instance_catalog"),
    BUNDLE_DIRECTORY("bundle_directory"),
    SCHEMA_INVENTORY("schema_inventory");

    private final String value;

    PerspectiveCode(String value) {
        this.value = value;
    }

    public String getValue() { return value; }

    public static PerspectiveCode fromString(String value) {
        if (value == null) return null;
        for (PerspectiveCode code : values()) {
            if (code.value.equals(value)) {
                return code;
            }
        }
        throw new IllegalArgumentException("Unknown perspective code: " + value);
    }

    @Override
    public String toString() { return value; }
}
