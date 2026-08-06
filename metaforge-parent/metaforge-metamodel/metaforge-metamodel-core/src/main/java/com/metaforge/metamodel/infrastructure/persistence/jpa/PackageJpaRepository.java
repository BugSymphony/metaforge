package com.metaforge.metamodel.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Package JPA Repository。
 */
@Repository
public interface PackageJpaRepository extends JpaRepository<PackageJpo, Long>,
        JpaSpecificationExecutor<PackageJpo> {

    Optional<PackageJpo> findByFqn(String fqn);

    List<PackageJpo> findByBundleVersionFqn(String bundleVersionFqn);

    List<PackageJpo> findByParentPackageFqn(String parentPackageFqn);

    boolean existsByFqnStartingWith(String fqnPrefix);
}
