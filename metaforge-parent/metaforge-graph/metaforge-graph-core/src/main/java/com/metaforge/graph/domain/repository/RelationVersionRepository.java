package com.metaforge.graph.domain.repository;

import com.metaforge.graph.domain.model.aggregate.RelationVersion;
import java.util.List;
import java.util.Optional;

/**
 * 历史表仓储端口接口（仅 INSERT 操作）。
 */
public interface RelationVersionRepository {

    RelationVersion save(RelationVersion version);

    List<RelationVersion> findByFqnOrderByVersionDesc(String fqn);

    Optional<RelationVersion> findByFqnAndVersion(String fqn, Integer version);

    List<RelationVersion> findByFqnIn(List<String> fqns);
}
