package com.metaforge.metamodel.application.service;

import com.metaforge.metamodel.api.dto.response.ImportResultDto;
import com.metaforge.metamodel.api.service.ImportExportService;
import com.metaforge.metamodel.domain.service.ExportService;
import com.metaforge.metamodel.domain.service.FqnGenerator;
import com.metaforge.metamodel.domain.service.ImportService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ImportExportServiceImpl implements ImportExportService {

    private final ExportService exportService;
    private final ImportService importService;
    private final FqnGenerator fqnGenerator;

    public ImportExportServiceImpl(ExportService exportService,
                                    ImportService importService,
                                    FqnGenerator fqnGenerator) {
        this.exportService = exportService;
        this.importService = importService;
        this.fqnGenerator = fqnGenerator;
    }

    @Override
    @Transactional(readOnly = true)
    public String exportBundle(String bundleFqn, String format) {
        Map<String, Object> data = exportService.exportBundleFull(bundleFqn);
        return exportService.format(data, format);
    }

    @Override
    @Transactional(readOnly = true)
    public String exportPackage(String packageFqn, String format) {
        String bundleCode = fqnGenerator.toBundleCode(packageFqn);
        Map<String, Object> data = exportService.exportBundleFull(bundleCode);
        return exportService.format(data, format);
    }

    @Override
    public ImportResultDto importMetamodel(String content, String format,
                                            String conflictStrategy) {
        Map<String, Object> result = importService.importMetamodel(content, format,
                conflictStrategy);
        int imported = ((Number) result.get("imported")).intValue();
        int skipped = ((Number) result.get("skipped")).intValue();
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) result.get("errors");

        if (errors.isEmpty()) {
            return ImportResultDto.success(imported, skipped);
        }
        return ImportResultDto.failure(errors);
    }
}
