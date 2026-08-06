package com.metaforge.metamodel.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExportManifestJpaRepository extends JpaRepository<ExportManifestJpo, Long> {

    Optional<ExportManifestJpo> findByBundleVersionFqn(String bundleVersionFqn);
}
