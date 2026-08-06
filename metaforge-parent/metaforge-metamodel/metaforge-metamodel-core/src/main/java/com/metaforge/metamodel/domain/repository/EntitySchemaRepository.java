package com.metaforge.metamodel.domain.repository;

import com.metaforge.metamodel.domain.model.entity.EntitySchema;

import java.util.List;
import java.util.Optional;

public interface EntitySchemaRepository {

    EntitySchema save(EntitySchema entity);

    Optional<EntitySchema> findByFqn(String fqn);

    List<EntitySchema> findByFqnStartingWith(String fqnPrefix);

    List<EntitySchema> findByPackageFqn(String packageFqn);

    List<EntitySchema> findByBundleVersionFqn(String bundleVersionFqn);

    void delete(EntitySchema entity);
}
