package com.metaforge.graph.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * RelationInstance JPA Repository。
 */
@Repository
public interface RelationInstanceJpaRepository extends JpaRepository<RelationInstanceJpo, Long>,
        JpaSpecificationExecutor<RelationInstanceJpo> {

    Optional<RelationInstanceJpo> findByFqn(String fqn);

    List<RelationInstanceJpo> findBySourceEntityFqn(String sourceEntityFqn);

    List<RelationInstanceJpo> findByTargetEntityFqn(String targetEntityFqn);

    List<RelationInstanceJpo> findByFqnStartingWith(String fqnPrefix);

    List<RelationInstanceJpo> findByRelationType(String relationType);

    List<RelationInstanceJpo> findByRelationSchemaFqn(String relationSchemaFqn);

    boolean existsByFqn(String fqn);

    void deleteByFqn(String fqn);

    long countByRelationTypeAndSourceEntityFqn(String relationType, String sourceEntityFqn);

    long countByRelationTypeAndTargetEntityFqn(String relationType, String targetEntityFqn);
}
