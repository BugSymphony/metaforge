package com.metaforge.metadata.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MetadataEntityDraftJpaRepository extends JpaRepository<MetadataEntityDraftJpo, Long> {

    Optional<MetadataEntityDraftJpo> findByFqn(String fqn);

    List<MetadataEntityDraftJpo> findByFqnIn(List<String> fqns);

    boolean existsByFqn(String fqn);

    void deleteByFqn(String fqn);
}
