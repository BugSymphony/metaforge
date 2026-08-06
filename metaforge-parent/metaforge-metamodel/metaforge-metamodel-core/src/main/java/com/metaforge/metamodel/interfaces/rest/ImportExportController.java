package com.metaforge.metamodel.interfaces.rest;

import com.metaforge.common.dto.ApiResponse;
import com.metaforge.metamodel.api.dto.response.ImportResultDto;
import com.metaforge.metamodel.api.service.ImportExportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/metamodel")
@Tag(name = "metamodel-import-export")
public class ImportExportController {

    private final ImportExportService importExportService;

    public ImportExportController(ImportExportService importExportService) {
        this.importExportService = importExportService;
    }

    @GetMapping("/export/bundle/{fqn}")
    @Operation(summary = "导出 Bundle 或 Package 级元模型")
    public ApiResponse<String> exportMetamodel(
            @PathVariable String fqn,
            @RequestParam(defaultValue = "JSON") String format) {
        String result;
        if (fqn.contains(":")) {
            result = importExportService.exportPackage(fqn, format);
        } else {
            result = importExportService.exportBundle(fqn, format);
        }
        return ApiResponse.success(result, "导出成功");
    }

    @PostMapping("/import")
    @Operation(summary = "声明式批量导入元模型")
    public ApiResponse<ImportResultDto> importMetamodel(
            @RequestBody Map<String, String> body) {
        String content = body.get("content");
        String format = body.getOrDefault("format", "JSON");
        String strategy = body.getOrDefault("conflictStrategy", "skip");
        ImportResultDto result = importExportService.importMetamodel(content, format, strategy);
        return ApiResponse.success(result, "导入完成");
    }
}
