package com.metaforge.metamodel.application.service;

import com.metaforge.metamodel.api.dto.request.UpdateExportManifestRequest;
import com.metaforge.metamodel.api.dto.response.ExportManifestDto;
import com.metaforge.metamodel.api.service.ExportManifestService;
import com.metaforge.metamodel.domain.model.aggregate.ExportManifest;
import com.metaforge.metamodel.domain.repository.ExportManifestRepository;
import com.metaforge.metamodel.domain.service.ExportValidationService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class ExportManifestServiceImpl implements ExportManifestService {

    private final ExportManifestRepository manifestRepository;
    private final ExportValidationService validationService;

    public ExportManifestServiceImpl(ExportManifestRepository manifestRepository,
                                      ExportValidationService validationService) {
        this.manifestRepository = manifestRepository;
        this.validationService = validationService;
    }

    @Override
    public ExportManifestDto update(String versionFqn, UpdateExportManifestRequest request) {
        // 校验导出清单合法性
        validationService.validateExport(versionFqn, request.getPackageFqns());

        ExportManifest manifest = manifestRepository.findByBundleVersionFqn(versionFqn)
                .orElseGet(() -> ExportManifest.create(versionFqn, request.getPackageFqns()));

        manifest.updateExportedPackages(request.getPackageFqns());
        ExportManifest saved = manifestRepository.save(manifest);
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExportManifestDto> findByVersionFqn(String versionFqn) {
        return manifestRepository.findByBundleVersionFqn(versionFqn).map(this::toDto);
    }

    private ExportManifestDto toDto(ExportManifest m) {
        ExportManifestDto dto = new ExportManifestDto();
        dto.setBundleVersionFqn(m.getBundleVersionFqn());
        dto.setExportedPackageFqns(m.getExportedPackageFqns());
        dto.setCreatedTime(m.getCreatedTime());
        dto.setUpdatedTime(m.getUpdatedTime());
        return dto;
    }
}
