package com.metaforge.metamodel.domain.repository;

import com.metaforge.metamodel.api.enums.VersionStatus;
import com.metaforge.metamodel.domain.model.aggregate.BundleVersion;

import java.util.List;
import java.util.Optional;

/**
 * BundleVersion 仓储端口接口（领域层）。
 */
public interface BundleVersionRepository {

    BundleVersion save(BundleVersion version);

    Optional<BundleVersion> findByFqn(String fqn);

    List<BundleVersion> findByBundleFqnAndStatus(String bundleFqn, VersionStatus status);

    Optional<BundleVersion> findTopByBundleFqnAndStatusOrderByCreatedTimeDesc(
            String bundleFqn, VersionStatus status);

    boolean existsByBundleFqnAndStatus(String bundleFqn, VersionStatus status);

    List<BundleVersion> findByBundleFqnOrderByCreatedTimeDesc(String bundleFqn);
}
