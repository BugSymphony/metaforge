package com.metaforge.metamodel.interfaces.rest;

import com.metaforge.common.dto.ApiResponse;
import com.metaforge.metamodel.api.dto.request.UpdateExportManifestRequest;
import com.metaforge.metamodel.api.dto.response.ExportManifestDto;
import com.metaforge.metamodel.api.service.ExportManifestService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/metamodel")
@Tag(name = "metamodel-export-manifest")
public class ExportManifestController {

    private final ExportManifestService manifestService;

    public ExportManifestController(ExportManifestService manifestService) {
        this.manifestService = manifestService;
    }

    @PutMapping("/versions/{fqn}/export-manifest")
    @Operation(summary = "更新导出清单")
    public ApiResponse<ExportManifestDto> update(
            @PathVariable String fqn, @RequestBody UpdateExportManifestRequest request) {
        return ApiResponse.success(manifestService.update(fqn, request), "导出清单更新成功");
    }

    @GetMapping("/versions/{fqn}/export-manifest")
    @Operation(summary = "查询导出清单")
    public ApiResponse<ExportManifestDto> get(@PathVariable String fqn) {
        Optional<ExportManifestDto> dto = manifestService.findByVersionFqn(fqn);
        return dto.map(m -> ApiResponse.success(m))
                .orElseGet(() -> ApiResponse.error(30102, "导出清单不存在: " + fqn));
    }
}
