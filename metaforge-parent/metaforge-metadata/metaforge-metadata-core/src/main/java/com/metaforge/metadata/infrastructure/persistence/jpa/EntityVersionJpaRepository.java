package com.metaforge.metadata.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntityVersionJpaRepository extends JpaRepository<EntityVersionJpo, Long> {

    List<EntityVersionJpo> findByFqnOrderByVersionDesc(String fqn);

    Optional<EntityVersionJpo> findByFqnAndVersion(String fqn, int version);

    List<EntityVersionJpo> findByFqnIn(List<String> fqns);
}
