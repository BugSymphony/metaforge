package com.metaforge.graph.interfaces.rest;

import com.metaforge.common.dto.ApiResponse;
import com.metaforge.graph.api.dto.RelationCount;
import com.metaforge.graph.api.dto.TopologyValidationReport;
import com.metaforge.graph.api.dto.TopologyValidationRequest;
import com.metaforge.graph.api.service.RelationTopologyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 拓扑查询 REST Controller。
 */
@RestController
@RequestMapping("/api/v1/graph")
@Tag(name = "语义关系管理")
public class RelationTopologyController {

    private final RelationTopologyService topologyService;

    public RelationTopologyController(RelationTopologyService topologyService) {
        this.topologyService = topologyService;
    }

    @GetMapping("/topology/dependent-relations")
    @Operation(summary = "查询实体关联依赖关系")
    public ApiResponse<List<String>> getDependentRelations(@RequestParam String entityFqn) {
        return ApiResponse.success(topologyService.getDependentRelations(entityFqn));
    }

    @PostMapping("/topology/validate")
    @Operation(summary = "批量拓扑完整性校验")
    public ApiResponse<TopologyValidationReport> validateTopology(@RequestBody TopologyValidationRequest request) {
        return ApiResponse.success(topologyService.validateTopology(request));
    }

    @GetMapping("/topology/relation-count")
    @Operation(summary = "查询实体关系计数")
    public ApiResponse<RelationCount> getRelationCount(@RequestParam String entityFqn) {
        return ApiResponse.success(topologyService.getRelationCount(entityFqn));
    }
}
