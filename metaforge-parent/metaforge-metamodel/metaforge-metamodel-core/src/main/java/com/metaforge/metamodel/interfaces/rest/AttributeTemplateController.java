package com.metaforge.metamodel.interfaces.rest;

import com.metaforge.common.dto.ApiResponse;
import com.metaforge.metamodel.api.dto.request.CreateAttributeTemplateRequest;
import com.metaforge.metamodel.api.dto.response.AttributeTemplateDto;
import com.metaforge.metamodel.api.service.ElementDefinitionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/metamodel")
@Tag(name = "metamodel-attribute-template")
public class AttributeTemplateController {

    private final ElementDefinitionService service;

    public AttributeTemplateController(ElementDefinitionService service) { this.service = service; }

    @PostMapping("/attribute-templates")
    @Operation(summary = "创建 AttributeTemplate")
    public ApiResponse<AttributeTemplateDto> create(@RequestBody CreateAttributeTemplateRequest request) {
        return ApiResponse.success(service.createAttributeTemplate(request), "AttributeTemplate 创建成功");
    }

    @GetMapping("/attribute-templates/{fqn}")
    @Operation(summary = "按 FQN 查询 AttributeTemplate")
    public ApiResponse<AttributeTemplateDto> get(@PathVariable String fqn) {
        Optional<AttributeTemplateDto> dto = service.findAttributeTemplateByFqn(fqn);
        return dto.map(d -> ApiResponse.success(d))
                .orElseGet(() -> ApiResponse.error(30102, "AttributeTemplate 不存在: " + fqn));
    }

    @DeleteMapping("/attribute-templates/{fqn}")
    @Operation(summary = "删除 AttributeTemplate")
    public ApiResponse<Void> delete(@PathVariable String fqn) {
        service.deleteAttributeTemplate(fqn);
        return ApiResponse.success(null, "AttributeTemplate 删除成功");
    }
}
