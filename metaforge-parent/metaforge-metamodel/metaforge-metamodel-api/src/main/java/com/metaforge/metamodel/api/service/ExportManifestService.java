package com.metaforge.metamodel.api.service;

import com.metaforge.metamodel.api.dto.request.UpdateExportManifestRequest;
import com.metaforge.metamodel.api.dto.response.ExportManifestDto;

import java.util.Optional;

public interface ExportManifestService {

    ExportManifestDto update(String versionFqn, UpdateExportManifestRequest request);

    Optional<ExportManifestDto> findByVersionFqn(String versionFqn);
}
