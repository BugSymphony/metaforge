package com.metaforge.graph.interfaces.rest;

import com.metaforge.common.dto.ApiResponse;
import com.metaforge.graph.api.dto.DeactivationCheckResult;
import com.metaforge.graph.api.dto.RelationInstanceDto;
import com.metaforge.graph.api.service.RelationActivationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 版本生效与下线 REST Controller。
 */
@RestController
@RequestMapping("/api/v1/graph")
@Tag(name = "语义关系管理")
public class RelationActivationController {

    private final RelationActivationService activationService;

    public RelationActivationController(RelationActivationService activationService) {
        this.activationService = activationService;
    }

    @PostMapping("/relations/activate")
    @Operation(summary = "执行草稿生效")
    public ApiResponse<RelationInstanceDto> activate(@RequestBody Map<String, String> body) {
        String fqn = body.get("fqn");
        RelationInstanceDto dto = activationService.activate(fqn);
        return ApiResponse.success(dto);
    }

    @PostMapping("/relations/deprecate")
    @Operation(summary = "执行关系下线")
    public ApiResponse<Void> deprecate(@RequestBody Map<String, String> body) {
        String fqn = body.get("fqn");
        activationService.deprecate(fqn);
        return ApiResponse.success(null);
    }

    @PostMapping("/relations/reactivate")
    @Operation(summary = "重新生效（基于历史版本）")
    public ApiResponse<RelationInstanceDto> reactivate(@RequestBody Map<String, String> body) {
        String fqn = body.get("fqn");
        RelationInstanceDto dto = activationService.reactivate(fqn);
        return ApiResponse.success(dto);
    }

    @PostMapping("/relations/check-deprecation")
    @Operation(summary = "校验下线前置条件")
    public ApiResponse<DeactivationCheckResult> checkDeprecation(@RequestBody Map<String, String> body) {
        String fqn = body.get("fqn");
        DeactivationCheckResult result = activationService.checkDeactivationPreconditions(fqn);
        return ApiResponse.success(result);
    }
}
