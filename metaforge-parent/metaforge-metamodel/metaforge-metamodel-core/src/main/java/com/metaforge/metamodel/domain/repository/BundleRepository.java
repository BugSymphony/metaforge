package com.metaforge.metamodel.domain.repository;

import com.metaforge.metamodel.domain.model.aggregate.Bundle;

import java.util.List;
import java.util.Optional;

/**
 * Bundle 仓储端口接口（领域层）。
 */
public interface BundleRepository {

    Bundle save(Bundle bundle);

    Optional<Bundle> findByFqn(String fqn);

    boolean existsByFqn(String fqn);

    List<Bundle> findAll();

    void delete(Bundle bundle);
}
