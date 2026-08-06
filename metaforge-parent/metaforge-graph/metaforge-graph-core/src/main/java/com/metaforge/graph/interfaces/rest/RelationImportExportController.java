package com.metaforge.graph.interfaces.rest;

import com.metaforge.common.dto.ApiResponse;
import com.metaforge.graph.api.dto.ExportResultDto;
import com.metaforge.graph.api.dto.ImportRequest;
import com.metaforge.graph.api.dto.ImportResultDto;
import com.metaforge.graph.api.service.RelationImportExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 导入导出 REST Controller。
 */
@RestController
@RequestMapping("/api/v1/graph")
@Tag(name = "语义关系管理")
public class RelationImportExportController {

    private final RelationImportExportService importExportService;

    public RelationImportExportController(RelationImportExportService importExportService) {
        this.importExportService = importExportService;
    }

    @PostMapping("/import")
    @Operation(summary = "批量导入")
    public ApiResponse<ImportResultDto> importRelations(@RequestBody ImportRequest request) {
        return ApiResponse.success(importExportService.importRelations(request));
    }

    @PostMapping("/export")
    @Operation(summary = "按条件导出")
    public ApiResponse<ExportResultDto> exportRelations(@RequestBody Map<String, Object> body) {
        String format = (String) body.getOrDefault("format", "JSON");

        if (body.containsKey("fqnPrefixes")) {
            @SuppressWarnings("unchecked")
            List<String> prefixes = (List<String>) body.get("fqnPrefixes");
            return ApiResponse.success(importExportService.exportByFqnPrefixes(prefixes, format));
        }

        if (body.containsKey("relationTypes")) {
            @SuppressWarnings("unchecked")
            List<String> types = (List<String>) body.get("relationTypes");
            return ApiResponse.success(importExportService.exportByRelationTypes(types, format));
        }

        if (body.containsKey("fqns")) {
            @SuppressWarnings("unchecked")
            List<String> fqns = (List<String>) body.get("fqns");
            return ApiResponse.success(importExportService.exportByFqns(fqns, format));
        }

        return ApiResponse.success(importExportService.exportByFqns(Collections.emptyList(), format));
    }
}
