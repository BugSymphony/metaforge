package com.metaforge.metadata.infrastructure.persistence.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MetadataEntityJpaRepository extends JpaRepository<MetadataEntityJpo, Long>,
        JpaSpecificationExecutor<MetadataEntityJpo> {

    Optional<MetadataEntityJpo> findByFqn(String fqn);

    List<MetadataEntityJpo> findByFqnIn(List<String> fqns);

    Page<MetadataEntityJpo> findByFqnIn(List<String> fqns, Pageable pageable);

    List<MetadataEntityJpo> findByEntitySchemaFqn(String entitySchemaFqn);

    Page<MetadataEntityJpo> findByEntitySchemaFqn(String entitySchemaFqn, Pageable pageable);

    boolean existsByFqn(String fqn);

    boolean existsByFqnStartingWith(String fqnPrefix);

    void deleteByFqn(String fqn);

    @Query(value = "SELECT * FROM metadata_management.metadata_entity WHERE content @> CAST(:conditionJson AS jsonb)",
            nativeQuery = true)
    List<MetadataEntityJpo> findByContentExactMatch(@Param("conditionJson") String conditionJson);

    @Query(value = "SELECT * FROM metadata_management.metadata_entity WHERE content @> CAST(:conditionJson AS jsonb)",
            countQuery = "SELECT count(*) FROM metadata_management.metadata_entity WHERE content @> CAST(:conditionJson AS jsonb)",
            nativeQuery = true)
    Page<MetadataEntityJpo> findByContentExactMatch(@Param("conditionJson") String conditionJson, Pageable pageable);
}
