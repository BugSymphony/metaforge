package com.metaforge.graph.interfaces.rest;

import com.metaforge.common.dto.ApiResponse;
import com.metaforge.graph.api.dto.CreateDraftRequest;
import com.metaforge.graph.api.dto.RelationInstanceDraftDto;
import com.metaforge.graph.api.dto.UpdateDraftContentRequest;
import com.metaforge.graph.api.service.RelationDraftService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * 草稿管理 REST Controller。
 */
@RestController
@RequestMapping("/api/v1/graph")
@Tag(name = "语义关系管理")
public class RelationDraftController {

    private final RelationDraftService draftService;

    public RelationDraftController(RelationDraftService draftService) {
        this.draftService = draftService;
    }

    @PostMapping("/drafts")
    @Operation(summary = "创建关系草稿")
    public ApiResponse<RelationInstanceDraftDto> createDraft(@RequestBody CreateDraftRequest request) {
        RelationInstanceDraftDto dto = draftService.createDraft(request);
        return ApiResponse.success(dto);
    }

    @PostMapping("/drafts/from-active")
    @Operation(summary = "基于生效版本创建草稿")
    public ApiResponse<RelationInstanceDraftDto> createDraftFromActive(@RequestParam String fqn) {
        RelationInstanceDraftDto dto = draftService.createDraftFromActive(fqn);
        return ApiResponse.success(dto);
    }

    @PutMapping("/drafts/{fqn}/content")
    @Operation(summary = "更新草稿内容")
    public ApiResponse<RelationInstanceDraftDto> updateDraftContent(
            @PathVariable String fqn, @RequestBody UpdateDraftContentRequest request) {
        RelationInstanceDraftDto dto = draftService.updateDraftContent(fqn, request);
        return ApiResponse.success(dto);
    }

    @GetMapping("/drafts/{fqn}")
    @Operation(summary = "查询草稿详情")
    public ApiResponse<RelationInstanceDraftDto> getDraft(@PathVariable String fqn) {
        RelationInstanceDraftDto dto = draftService.getDraft(fqn);
        return ApiResponse.success(dto);
    }

    @DeleteMapping("/drafts/{fqn}")
    @Operation(summary = "物理删除草稿")
    public ApiResponse<Void> deleteDraft(@PathVariable String fqn) {
        draftService.deleteDraft(fqn);
        return ApiResponse.success(null);
    }
}
