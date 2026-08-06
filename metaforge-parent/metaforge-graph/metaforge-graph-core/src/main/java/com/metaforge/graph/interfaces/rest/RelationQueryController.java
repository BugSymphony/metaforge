package com.metaforge.graph.interfaces.rest;

import com.metaforge.common.dto.ApiResponse;
import com.metaforge.common.dto.PageRequest;
import com.metaforge.common.dto.PageResult;
import com.metaforge.graph.api.dto.AdminQueryRequest;
import com.metaforge.graph.api.dto.RelationInstanceDto;
import com.metaforge.graph.api.dto.RelationQueryRequest;
import com.metaforge.graph.api.service.RelationQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 关系查询 REST Controller。
 */
@RestController
@RequestMapping("/api/v1/graph")
@Tag(name = "语义关系管理")
public class RelationQueryController {

    private final RelationQueryService queryService;

    public RelationQueryController(RelationQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/relations/{fqn}")
    @Operation(summary = "FQN 精准查询")
    public ApiResponse<RelationInstanceDto> getByFqn(@PathVariable String fqn) {
        return ApiResponse.success(queryService.getByFqn(fqn));
    }

    @GetMapping("/relations/outbound")
    @Operation(summary = "实体出边查询")
    public ApiResponse<List<RelationInstanceDto>> getOutboundRelations(
            @RequestParam String entityFqn,
            @RequestParam(required = false) String relationType,
            @RequestParam(required = false) String targetEntityType) {
        return ApiResponse.success(queryService.getOutboundRelations(entityFqn, relationType, targetEntityType));
    }

    @GetMapping("/relations/inbound")
    @Operation(summary = "实体入边查询")
    public ApiResponse<List<RelationInstanceDto>> getInboundRelations(
            @RequestParam String entityFqn,
            @RequestParam(required = false) String relationType,
            @RequestParam(required = false) String sourceEntityType) {
        return ApiResponse.success(queryService.getInboundRelations(entityFqn, relationType, sourceEntityType));
    }

    @GetMapping("/relations")
    @Operation(summary = "条件列表查询")
    public ApiResponse<PageResult<RelationInstanceDto>> listByConditions(
            @RequestParam(required = false) String fqnPrefix,
            @RequestParam(required = false) String relationSchemaFqn,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        PageRequest pr = new PageRequest(page, size, sort);
        return ApiResponse.success(queryService.listByConditions(fqnPrefix, relationSchemaFqn, pr));
    }

    @PostMapping("/relations/filter")
    @Operation(summary = "多维过滤查询")
    public ApiResponse<PageResult<RelationInstanceDto>> multiFilter(@RequestBody RelationQueryRequest request) {
        return ApiResponse.success(queryService.multiFilter(request));
    }

    @GetMapping("/admin/relations")
    @Operation(summary = "管理员全状态聚合查询")
    public ApiResponse<PageResult<RelationInstanceDto>> adminQuery(
            @RequestParam(required = false) String fqnPrefix,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        AdminQueryRequest request = new AdminQueryRequest();
        request.setFqnPrefix(fqnPrefix);
        request.setPageRequest(new PageRequest(page, size));
        return ApiResponse.success(queryService.adminQuery(request));
    }
}
