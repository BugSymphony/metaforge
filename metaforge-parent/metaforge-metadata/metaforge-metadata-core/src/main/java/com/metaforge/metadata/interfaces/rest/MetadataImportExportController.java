package com.metaforge.metadata.interfaces.rest;

import com.metaforge.common.dto.ApiResponse;
import com.metaforge.metadata.api.dto.request.ExportRequest;
import com.metaforge.metadata.api.dto.request.ImportRequest;
import com.metaforge.metadata.api.dto.response.ExportResultDto;
import com.metaforge.metadata.api.dto.response.ImportResultDto;
import com.metaforge.metadata.api.service.MetadataImportExportService;
import com.metaforge.metadata.application.service.MetadataImportExportServiceImpl;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/metadata")
@Tag(name = "metadata-management")
public class MetadataImportExportController {

    private final MetadataImportExportService importExportService;
    private final MetadataImportExportServiceImpl importExportServiceImpl;

    public MetadataImportExportController(MetadataImportExportService importExportService,
                                          MetadataImportExportServiceImpl importExportServiceImpl) {
        this.importExportService = importExportService;
        this.importExportServiceImpl = importExportServiceImpl;
    }

    @PostMapping("/import")
    public ApiResponse<ImportResultDto> importMetadata(@RequestBody ImportRequest request) {
        ImportResultDto result = importExportService.importMetadata(request);
        return ApiResponse.success(result);
    }

    @PostMapping("/export")
    public ApiResponse<ExportResultDto> exportMetadata(@RequestBody ExportRequest request) {
        ExportResultDto result;

        if (request.getFqns() != null && !request.getFqns().isEmpty()) {
            result = importExportService.exportByFqns(request);
        } else if (request.getEntitySchemaFqn() != null && !request.getEntitySchemaFqn().isEmpty()) {
            result = importExportService.exportByEntitySchema(request);
        } else {
            result = importExportService.exportByFqnPrefixes(request);
        }

        return ApiResponse.success(result);
    }

    @PostMapping("/validate-batch")
    public ApiResponse<ImportResultDto> validateBatch(
            @RequestParam(required = false) String entitySchemaFqn,
            @RequestParam(required = false) List<String> fqnPrefixes) {
        ImportResultDto result = importExportServiceImpl.validateBatch(entitySchemaFqn, fqnPrefixes);
        return ApiResponse.success(result);
    }
}
