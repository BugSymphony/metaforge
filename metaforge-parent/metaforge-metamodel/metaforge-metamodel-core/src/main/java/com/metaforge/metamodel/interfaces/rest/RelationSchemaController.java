package com.metaforge.metamodel.interfaces.rest;

import com.metaforge.common.dto.ApiResponse;
import com.metaforge.metamodel.api.dto.ElementQueryRequest;
import com.metaforge.metamodel.api.dto.request.CreateRelationSchemaRequest;
import com.metaforge.metamodel.api.dto.response.RelationSchemaDto;
import com.metaforge.metamodel.api.service.ElementDefinitionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/metamodel")
@Tag(name = "metamodel-relation-schema")
public class RelationSchemaController {

    private final ElementDefinitionService service;

    public RelationSchemaController(ElementDefinitionService service) { this.service = service; }

    @PostMapping("/relation-schemas")
    @Operation(summary = "创建 RelationSchema")
    public ApiResponse<RelationSchemaDto> create(@RequestBody CreateRelationSchemaRequest request) {
        return ApiResponse.success(service.createRelationSchema(request), "RelationSchema 创建成功");
    }

    @GetMapping("/relation-schemas/{fqn}")
    @Operation(summary = "按 FQN 查询 RelationSchema")
    public ApiResponse<RelationSchemaDto> get(@PathVariable String fqn) {
        Optional<RelationSchemaDto> dto = service.findRelationSchemaByFqn(fqn);
        return dto.map(d -> ApiResponse.success(d))
                .orElseGet(() -> ApiResponse.error(30102, "RelationSchema 不存在: " + fqn));
    }

    @DeleteMapping("/relation-schemas/{fqn}")
    @Operation(summary = "删除 RelationSchema")
    public ApiResponse<Void> delete(@PathVariable String fqn) {
        service.deleteRelationSchema(fqn);
        return ApiResponse.success(null, "RelationSchema 删除成功");
    }

    @GetMapping("/relation-schemas")
    @Operation(summary = "按 FQN 前缀集合查询 RelationSchema 列表")
    public ApiResponse<List<RelationSchemaDto>> list(ElementQueryRequest request) {
        return ApiResponse.success(service.listRelationSchemas(request));
    }
}
