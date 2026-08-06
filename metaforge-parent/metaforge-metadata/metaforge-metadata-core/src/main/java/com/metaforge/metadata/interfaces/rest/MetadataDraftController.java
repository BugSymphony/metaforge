package com.metaforge.metadata.interfaces.rest;

import com.metaforge.common.dto.ApiResponse;
import com.metaforge.metadata.api.dto.request.CreateDraftRequest;
import com.metaforge.metadata.api.dto.request.UpdateDraftContentRequest;
import com.metaforge.metadata.api.dto.response.MetadataEntityDraftDto;
import com.metaforge.metadata.api.service.MetadataDraftService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/metadata")
@Tag(name = "metadata-management")
public class MetadataDraftController {

    private final MetadataDraftService metadataDraftService;

    public MetadataDraftController(MetadataDraftService metadataDraftService) {
        this.metadataDraftService = metadataDraftService;
    }

    @PostMapping("/drafts")
    public ApiResponse<MetadataEntityDraftDto> createDraft(@RequestBody CreateDraftRequest request) {
        MetadataEntityDraftDto result = metadataDraftService.createDraft(request);
        return ApiResponse.success(result, "草稿创建成功");
    }

    @GetMapping("/drafts/{fqn}")
    public ApiResponse<MetadataEntityDraftDto> getDraft(@PathVariable String fqn) {
        MetadataEntityDraftDto result = metadataDraftService.getDraft(fqn);
        return ApiResponse.success(result);
    }

    @PutMapping("/drafts/{fqn}/content")
    public ApiResponse<MetadataEntityDraftDto> updateDraftContent(
            @PathVariable String fqn,
            @RequestBody UpdateDraftContentRequest request) {
        MetadataEntityDraftDto result = metadataDraftService.updateDraftContent(fqn, request);
        return ApiResponse.success(result, "草稿内容更新成功");
    }

    @DeleteMapping("/drafts/{fqn}")
    public ApiResponse<Void> deleteDraft(@PathVariable String fqn) {
        metadataDraftService.deleteDraft(fqn);
        return ApiResponse.success(null, "草稿删除成功");
    }

    @PostMapping("/drafts/from-active/{fqn}")
    public ApiResponse<MetadataEntityDraftDto> createDraftFromActive(
            @PathVariable String fqn,
            @RequestParam(required = false) String createdBy) {
        MetadataEntityDraftDto result = metadataDraftService.createDraftFromActive(fqn, createdBy);
        return ApiResponse.success(result, "从生效版本创建草稿成功");
    }
}
