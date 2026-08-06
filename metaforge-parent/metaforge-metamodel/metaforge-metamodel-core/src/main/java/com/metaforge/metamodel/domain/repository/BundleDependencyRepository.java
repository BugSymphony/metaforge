package com.metaforge.metamodel.domain.repository;

import java.util.List;

/**
 * Bundle 依赖仓储端口接口（领域层）。
 */
public interface BundleDependencyRepository {

    void save(String sourceVersionFqn, String targetVersionFqn);

    List<String> findTargetFqnsBySource(String sourceVersionFqn);

    List<String> findSourceFqnsByTarget(String targetVersionFqn);

    boolean exists(String sourceVersionFqn, String targetVersionFqn);

    void delete(String sourceVersionFqn, String targetVersionFqn);

    List<String> findAllSourceFqns();
}
