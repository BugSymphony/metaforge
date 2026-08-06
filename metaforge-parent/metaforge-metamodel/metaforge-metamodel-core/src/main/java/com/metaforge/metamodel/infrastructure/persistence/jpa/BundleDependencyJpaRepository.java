package com.metaforge.metamodel.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * BundleDependency JPA Repository。
 */
@Repository
public interface BundleDependencyJpaRepository extends JpaRepository<BundleDependencyJpo, Long> {

    List<BundleDependencyJpo> findBySourceVersionFqn(String sourceVersionFqn);

    List<BundleDependencyJpo> findByTargetVersionFqn(String targetVersionFqn);

    boolean existsBySourceVersionFqnAndTargetVersionFqn(
            String sourceVersionFqn, String targetVersionFqn);

    void deleteBySourceVersionFqnAndTargetVersionFqn(
            String sourceVersionFqn, String targetVersionFqn);
}
