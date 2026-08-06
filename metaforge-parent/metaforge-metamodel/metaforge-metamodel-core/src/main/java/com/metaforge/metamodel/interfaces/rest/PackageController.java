package com.metaforge.metamodel.interfaces.rest;

import com.metaforge.common.dto.ApiResponse;
import com.metaforge.metamodel.api.dto.request.CreatePackageRequest;
import com.metaforge.metamodel.api.dto.response.PackageDto;
import com.metaforge.metamodel.api.service.PackageManagementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Package REST 控制器。
 */
@RestController
@RequestMapping("/api/v1/metamodel")
@Tag(name = "metamodel-package")
public class PackageController {

    private final PackageManagementService packageService;

    public PackageController(PackageManagementService packageService) {
        this.packageService = packageService;
    }

    @PostMapping("/packages")
    @Operation(summary = "创建 Package")
    public ApiResponse<PackageDto> createPackage(@RequestBody CreatePackageRequest request) {
        PackageDto dto = packageService.create(request);
        return ApiResponse.success(dto, "Package 创建成功");
    }

    @GetMapping("/packages/{fqn}")
    @Operation(summary = "按 FQN 查询 Package")
    public ApiResponse<PackageDto> getPackage(@PathVariable String fqn) {
        Optional<PackageDto> dto = packageService.findByFqn(fqn);
        return dto.map(p -> ApiResponse.success(p))
                .orElseGet(() -> ApiResponse.error(30102, "Package 不存在: " + fqn));
    }

    @GetMapping("/packages")
    @Operation(summary = "按 BundleVersion FQN 查询 Package 列表")
    public ApiResponse<List<PackageDto>> listPackages(
            @RequestParam("bundleVersionFqn") String bundleVersionFqn) {
        List<PackageDto> list = packageService.listByBundleVersion(bundleVersionFqn);
        return ApiResponse.success(list);
    }

    @DeleteMapping("/packages/{fqn}")
    @Operation(summary = "删除 Package（需为空）")
    public ApiResponse<Void> deletePackage(@PathVariable String fqn) {
        packageService.delete(fqn);
        return ApiResponse.success(null, "Package 删除成功");
    }
}
