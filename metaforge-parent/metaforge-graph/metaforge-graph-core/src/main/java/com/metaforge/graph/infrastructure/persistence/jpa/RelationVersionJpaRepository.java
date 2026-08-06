package com.metaforge.graph.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * RelationVersion JPA Repository。
 * 历史表仅允许 SELECT/INSERT，禁止 UPDATE/DELETE。
 */
@Repository
public interface RelationVersionJpaRepository extends JpaRepository<RelationVersionJpo, Long>,
        JpaSpecificationExecutor<RelationVersionJpo> {

    List<RelationVersionJpo> findByFqnOrderByVersionDesc(String fqn);

    Optional<RelationVersionJpo> findByFqnAndVersion(String fqn, Integer version);

    List<RelationVersionJpo> findByFqnIn(List<String> fqns);
}
