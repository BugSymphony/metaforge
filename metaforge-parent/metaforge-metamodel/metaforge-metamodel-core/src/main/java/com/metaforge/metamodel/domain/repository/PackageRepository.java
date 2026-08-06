package com.metaforge.metamodel.domain.repository;

import com.metaforge.metamodel.domain.model.entity.Package;

import java.util.List;
import java.util.Optional;

/**
 * Package 仓储端口接口（领域层）。
 */
public interface PackageRepository {

    Package save(Package pkg);

    Optional<Package> findByFqn(String fqn);

    List<Package> findByBundleVersionFqn(String bundleVersionFqn);

    List<Package> findByParentPackageFqn(String parentPackageFqn);

    boolean existsByFqnPrefix(String fqnPrefix);

    void delete(Package pkg);
}
