package com.metaforge.metamodel.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * BundleVersion JPA Repository。
 */
@Repository
public interface BundleVersionJpaRepository extends JpaRepository<BundleVersionJpo, Long> {

    Optional<BundleVersionJpo> findByFqn(String fqn);

    List<BundleVersionJpo> findByBundleFqnAndStatus(String bundleFqn, String status);

    Optional<BundleVersionJpo> findTopByBundleFqnAndStatusOrderByCreatedTimeDesc(
            String bundleFqn, String status);

    boolean existsByBundleFqnAndStatus(String bundleFqn, String status);

    List<BundleVersionJpo> findByBundleFqnOrderByCreatedTimeDesc(String bundleFqn);
}
