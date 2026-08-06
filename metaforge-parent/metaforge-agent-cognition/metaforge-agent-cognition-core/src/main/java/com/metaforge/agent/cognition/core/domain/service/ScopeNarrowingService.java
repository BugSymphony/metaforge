package com.metaforge.agent.cognition.core.domain.service;

import java.util.List;

public interface ScopeNarrowingService {

    NarrowedScope narrow(String entryEntityFqn);

    record NarrowedScope(
            List<String> blueprintStepFqns,
            List<String> relatedEntityFqns,
            List<String> relatedSchemaFqns) {}
}
