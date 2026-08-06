package com.metaforge.metadata.interfaces.rest;

import com.metaforge.common.dto.ApiResponse;
import com.metaforge.common.dto.PageRequest;
import com.metaforge.common.dto.PageResult;
import com.metaforge.metadata.api.dto.request.DiffRequest;
import com.metaforge.metadata.api.dto.response.EntityVersionDto;
import com.metaforge.metadata.api.dto.response.VersionDiffDto;
import com.metaforge.metadata.api.service.MetadataHistoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/metadata")
@Tag(name = "metadata-management")
public class MetadataHistoryController {

    private final MetadataHistoryService historyService;

    public MetadataHistoryController(MetadataHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping("/history/{fqn}/versions")
    public ApiResponse<PageResult<EntityVersionDto>> listVersions(
            @PathVariable String fqn,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        PageResult<EntityVersionDto> result = historyService.listVersions(fqn,
                new PageRequest(page, size, sort));
        return ApiResponse.success(result);
    }

    @GetMapping("/history/{fqn}/versions/{version}")
    public ApiResponse<EntityVersionDto> getVersionDetail(
            @PathVariable String fqn,
            @PathVariable int version) {
        EntityVersionDto result = historyService.getVersionDetail(fqn, version);
        return ApiResponse.success(result);
    }

    @PostMapping("/history/diff")
    public ApiResponse<VersionDiffDto> compareVersions(@RequestBody DiffRequest request) {
        VersionDiffDto result = historyService.compareVersions(request);
        return ApiResponse.success(result);
    }
}
