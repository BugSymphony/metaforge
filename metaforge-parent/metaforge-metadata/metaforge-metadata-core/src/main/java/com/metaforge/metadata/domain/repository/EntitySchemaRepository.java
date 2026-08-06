package com.metaforge.metadata.domain.repository;

import java.util.Map;
import java.util.Optional;

public interface EntitySchemaRepository {
    Optional<Map<String, Object>> getJsonSchema(String entitySchemaFqn);
    boolean existsByFqn(String entitySchemaFqn);
    boolean isPublished(String entitySchemaFqn);
}
