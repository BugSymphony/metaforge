package com.metaforge.metamodel.domain.repository;

import com.metaforge.metamodel.domain.model.entity.RelationSchema;

import java.util.List;
import java.util.Optional;

public interface RelationSchemaRepository {

    RelationSchema save(RelationSchema entity);

    Optional<RelationSchema> findByFqn(String fqn);

    List<RelationSchema> findByFqnStartingWith(String fqnPrefix);

    List<RelationSchema> findByPackageFqn(String packageFqn);

    List<RelationSchema> findByBundleVersionFqn(String bundleVersionFqn);

    void delete(RelationSchema entity);
}
