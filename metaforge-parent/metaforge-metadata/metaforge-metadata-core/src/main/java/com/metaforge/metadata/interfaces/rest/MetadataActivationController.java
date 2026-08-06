package com.metaforge.metadata.interfaces.rest;

import com.metaforge.common.dto.ApiResponse;
import com.metaforge.metadata.api.dto.response.DeactivationCheckResult;
import com.metaforge.metadata.api.dto.response.MetadataEntityDto;
import com.metaforge.metadata.api.service.MetadataActivationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/metadata")
@Tag(name = "metadata-management")
public class MetadataActivationController {

    private final MetadataActivationService activationService;

    public MetadataActivationController(MetadataActivationService activationService) {
        this.activationService = activationService;
    }

    @PostMapping("/entities/{fqn}/activate")
    public ApiResponse<MetadataEntityDto> activate(@PathVariable String fqn) {
        MetadataEntityDto result = activationService.activate(fqn);
        return ApiResponse.success(result, "生效成功");
    }

    @PostMapping("/entities/{fqn}/deactivate")
    public ApiResponse<Void> deactivate(@PathVariable String fqn) {
        activationService.deactivate(fqn);
        return ApiResponse.success(null, "下线成功");
    }

    @PostMapping("/entities/{fqn}/reactivate")
    public ApiResponse<MetadataEntityDto> reactivate(@PathVariable String fqn) {
        MetadataEntityDto result = activationService.reactivate(fqn);
        return ApiResponse.success(result, "重新生效成功");
    }

    @GetMapping("/entities/{fqn}/deactivation-check")
    public ApiResponse<DeactivationCheckResult> checkDeactivationPreconditions(@PathVariable String fqn) {
        DeactivationCheckResult result = activationService.checkDeactivationPreconditions(fqn);
        return ApiResponse.success(result);
    }
}
