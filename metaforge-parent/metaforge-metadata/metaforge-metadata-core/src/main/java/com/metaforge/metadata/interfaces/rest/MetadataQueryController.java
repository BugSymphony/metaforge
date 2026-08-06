package com.metaforge.metadata.interfaces.rest;

import com.metaforge.common.dto.ApiResponse;
import com.metaforge.common.dto.PageRequest;
import com.metaforge.common.dto.PageResult;
import com.metaforge.metadata.api.dto.request.AdminQueryRequest;
import com.metaforge.metadata.api.dto.request.AttributeCondition;
import com.metaforge.metadata.api.dto.request.MetadataQueryRequest;
import com.metaforge.metadata.api.dto.response.MetadataEntityDto;
import com.metaforge.metadata.api.service.MetadataQueryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/metadata")
@Tag(name = "metadata-management")
public class MetadataQueryController {

    private final MetadataQueryService queryService;

    public MetadataQueryController(MetadataQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/entities/{fqn}")
    public ApiResponse<MetadataEntityDto> getByFqn(@PathVariable String fqn) {
        MetadataEntityDto result = queryService.getByFqn(fqn);
        return ApiResponse.success(result);
    }

    @GetMapping("/entities/query/fqn-prefix")
    public ApiResponse<PageResult<MetadataEntityDto>> listByFqnPrefixes(
            @RequestParam List<String> prefixes,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        MetadataQueryRequest request = new MetadataQueryRequest();
        request.setFqnPrefixes(prefixes);
        request.setPageRequest(new PageRequest(page, size, sort));
        PageResult<MetadataEntityDto> result = queryService.listByFqnPrefixes(request);
        return ApiResponse.success(result);
    }

    @GetMapping("/entities/query/entity-schema")
    public ApiResponse<PageResult<MetadataEntityDto>> listByEntitySchema(
            @RequestParam String entitySchemaFqn,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        MetadataQueryRequest request = new MetadataQueryRequest();
        request.setEntitySchemaFqn(entitySchemaFqn);
        request.setPageRequest(new PageRequest(page, size, sort));
        PageResult<MetadataEntityDto> result = queryService.listByEntitySchema(request);
        return ApiResponse.success(result);
    }

    @GetMapping("/admin/metadata")
    public ApiResponse<PageResult<MetadataEntityDto>> adminQuery(
            @RequestParam(required = false) String fqn,
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        AdminQueryRequest request = new AdminQueryRequest();
        request.setFqn(fqn);
        request.setStatuses(statuses);
        request.setPageRequest(new PageRequest(page, size, sort));
        PageResult<MetadataEntityDto> result = queryService.adminQuery(request);
        return ApiResponse.success(result);
    }
}
