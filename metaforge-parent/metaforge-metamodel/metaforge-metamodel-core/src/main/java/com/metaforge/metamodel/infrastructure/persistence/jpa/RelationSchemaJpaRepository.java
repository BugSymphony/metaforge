package com.metaforge.metamodel.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RelationSchemaJpaRepository extends JpaRepository<RelationSchemaJpo, Long>,
        JpaSpecificationExecutor<RelationSchemaJpo> {

    Optional<RelationSchemaJpo> findByFqn(String fqn);

    List<RelationSchemaJpo> findByFqnStartingWith(String fqnPrefix);

    List<RelationSchemaJpo> findByPackageFqn(String packageFqn);

    List<RelationSchemaJpo> findByBundleVersionFqn(String bundleVersionFqn);
}
