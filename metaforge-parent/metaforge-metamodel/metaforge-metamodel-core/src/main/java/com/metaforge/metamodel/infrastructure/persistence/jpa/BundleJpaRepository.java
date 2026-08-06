package com.metaforge.metamodel.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Bundle JPA Repository。
 */
@Repository
public interface BundleJpaRepository extends JpaRepository<BundleJpo, Long>,
        JpaSpecificationExecutor<BundleJpo> {

    Optional<BundleJpo> findByFqn(String fqn);

    boolean existsByFqn(String fqn);
}
