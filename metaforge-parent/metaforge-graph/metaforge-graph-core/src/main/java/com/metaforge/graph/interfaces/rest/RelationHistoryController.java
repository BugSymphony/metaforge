package com.metaforge.graph.interfaces.rest;

import com.metaforge.common.dto.ApiResponse;
import com.metaforge.graph.api.dto.DiffRequest;
import com.metaforge.graph.api.dto.RelationVersionDto;
import com.metaforge.graph.api.dto.VersionDiffDto;
import com.metaforge.graph.api.service.RelationHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 历史追溯 REST Controller。
 */
@RestController
@RequestMapping("/api/v1/graph")
@Tag(name = "语义关系管理")
public class RelationHistoryController {

    private final RelationHistoryService historyService;

    public RelationHistoryController(RelationHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping("/versions/{fqn}")
    @Operation(summary = "查询全历史版本列表")
    public ApiResponse<List<RelationVersionDto>> listVersions(@PathVariable String fqn) {
        return ApiResponse.success(historyService.listVersions(fqn));
    }

    @GetMapping("/versions/{fqn}/{version}")
    @Operation(summary = "查询单版本详情")
    public ApiResponse<RelationVersionDto> getVersionDetail(
            @PathVariable String fqn, @PathVariable int version) {
        return ApiResponse.success(historyService.getVersionDetail(fqn, version));
    }

    @PostMapping("/versions/diff")
    @Operation(summary = "两版本差异对比")
    public ApiResponse<VersionDiffDto> compareVersions(@RequestBody DiffRequest request) {
        return ApiResponse.success(historyService.compareVersions(request));
    }
}
