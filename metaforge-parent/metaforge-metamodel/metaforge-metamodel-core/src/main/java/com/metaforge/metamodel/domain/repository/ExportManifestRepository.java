package com.metaforge.metamodel.domain.repository;

import com.metaforge.metamodel.domain.model.aggregate.ExportManifest;

import java.util.Optional;

public interface ExportManifestRepository {

    ExportManifest save(ExportManifest manifest);

    Optional<ExportManifest> findByBundleVersionFqn(String bundleVersionFqn);
}
