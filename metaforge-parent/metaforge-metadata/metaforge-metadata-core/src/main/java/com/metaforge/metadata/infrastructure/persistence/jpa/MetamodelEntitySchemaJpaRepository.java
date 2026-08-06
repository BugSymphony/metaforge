package com.metaforge.metadata.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MetamodelEntitySchemaJpaRepository extends JpaRepository<MetamodelEntitySchemaJpo, Long> {

    Optional<MetamodelEntitySchemaJpo> findByFqn(String fqn);

    boolean existsByFqn(String fqn);
}
