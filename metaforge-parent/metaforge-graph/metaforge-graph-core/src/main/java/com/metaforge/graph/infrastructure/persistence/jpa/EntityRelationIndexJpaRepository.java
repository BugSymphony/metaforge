package com.metaforge.graph.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * EntityRelationIndex JPA Repository。
 */
@Repository
public interface EntityRelationIndexJpaRepository extends JpaRepository<EntityRelationIndexJpo, Long>,
        JpaSpecificationExecutor<EntityRelationIndexJpo> {

    List<EntityRelationIndexJpo> findByEntityFqnAndDirection(String entityFqn, String direction);

    List<EntityRelationIndexJpo> findByRelationFqn(String relationFqn);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from EntityRelationIndexJpo e where e.relationFqn = :relationFqn")
    void deleteByRelationFqn(@org.springframework.data.repository.query.Param("relationFqn") String relationFqn);
}
