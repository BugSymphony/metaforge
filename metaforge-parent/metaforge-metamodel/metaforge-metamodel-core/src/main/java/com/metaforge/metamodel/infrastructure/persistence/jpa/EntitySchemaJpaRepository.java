package com.metaforge.metamodel.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntitySchemaJpaRepository extends JpaRepository<EntitySchemaJpo, Long>,
        JpaSpecificationExecutor<EntitySchemaJpo> {

    Optional<EntitySchemaJpo> findByFqn(String fqn);

    List<EntitySchemaJpo> findByFqnStartingWith(String fqnPrefix);

    List<EntitySchemaJpo> findByPackageFqn(String packageFqn);

    List<EntitySchemaJpo> findByBundleVersionFqn(String bundleVersionFqn);
}
