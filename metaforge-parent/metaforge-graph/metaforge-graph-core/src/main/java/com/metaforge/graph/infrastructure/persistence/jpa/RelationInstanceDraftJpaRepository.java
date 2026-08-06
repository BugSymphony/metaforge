package com.metaforge.graph.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * RelationInstanceDraft JPA Repository。
 */
@Repository
public interface RelationInstanceDraftJpaRepository extends JpaRepository<RelationInstanceDraftJpo, Long>,
        JpaSpecificationExecutor<RelationInstanceDraftJpo> {

    Optional<RelationInstanceDraftJpo> findByFqn(String fqn);

    List<RelationInstanceDraftJpo> findBySourceEntityFqn(String sourceEntityFqn);

    List<RelationInstanceDraftJpo> findByTargetEntityFqn(String targetEntityFqn);

    boolean existsByFqn(String fqn);

    void deleteByFqn(String fqn);
}
