package com.metaforge.metamodel.interfaces.rest;

import com.metaforge.common.dto.ApiResponse;
import com.metaforge.metamodel.api.dto.ElementQueryRequest;
import com.metaforge.metamodel.api.dto.request.CreateEntitySchemaRequest;
import com.metaforge.metamodel.api.dto.request.UpdateEntitySchemaRequest;
import com.metaforge.metamodel.api.dto.response.EntitySchemaDto;
import com.metaforge.metamodel.api.service.ElementDefinitionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/metamodel")
@Tag(name = "metamodel-entity-schema")
public class EntitySchemaController {

    private final ElementDefinitionService service;

    public EntitySchemaController(ElementDefinitionService service) { this.service = service; }

    @PostMapping("/entity-schemas")
    @Operation(summary = "创建 EntitySchema")
    public ApiResponse<EntitySchemaDto> create(@RequestBody CreateEntitySchemaRequest request) {
        return ApiResponse.success(service.createEntitySchema(request), "EntitySchema 创建成功");
    }

    @GetMapping("/entity-schemas/{fqn}")
    @Operation(summary = "按 FQN 查询 EntitySchema")
    public ApiResponse<EntitySchemaDto> get(@PathVariable String fqn) {
        Optional<EntitySchemaDto> dto = service.findEntitySchemaByFqn(fqn);
        return dto.map(d -> ApiResponse.success(d))
                .orElseGet(() -> ApiResponse.error(30102, "EntitySchema 不存在: " + fqn));
    }

    @PutMapping("/entity-schemas/{fqn}")
    @Operation(summary = "更新 EntitySchema")
    public ApiResponse<EntitySchemaDto> update(@PathVariable String fqn,
                                                @RequestBody UpdateEntitySchemaRequest request) {
        return ApiResponse.success(service.updateEntitySchema(fqn, request), "EntitySchema 更新成功");
    }

    @DeleteMapping("/entity-schemas/{fqn}")
    @Operation(summary = "删除 EntitySchema")
    public ApiResponse<Void> delete(@PathVariable String fqn) {
        service.deleteEntitySchema(fqn);
        return ApiResponse.success(null, "EntitySchema 删除成功");
    }

    @GetMapping("/entity-schemas")
    @Operation(summary = "按 FQN 前缀集合查询 EntitySchema 列表")
    public ApiResponse<List<EntitySchemaDto>> list(ElementQueryRequest request) {
        return ApiResponse.success(service.listEntitySchemas(request));
    }
}
