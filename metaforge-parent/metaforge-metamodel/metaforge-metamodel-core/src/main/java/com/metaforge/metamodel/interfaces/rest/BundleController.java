package com.metaforge.metamodel.interfaces.rest;

import com.metaforge.common.dto.ApiResponse;
import com.metaforge.metamodel.api.dto.request.CreateBundleRequest;
import com.metaforge.metamodel.api.dto.request.CreateDraftRequest;
import com.metaforge.metamodel.api.dto.response.BundleDto;
import com.metaforge.metamodel.api.dto.response.BundleVersionDto;
import com.metaforge.metamodel.api.service.BundleManagementService;
import com.metaforge.metamodel.api.service.BundleVersionManagementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Bundle 与 BundleVersion REST 控制器。
 */
@RestController
@RequestMapping("/api/v1/metamodel")
@Tag(name = "metamodel-bundle")
public class BundleController {

    private final BundleManagementService bundleService;
    private final BundleVersionManagementService versionService;

    public BundleController(BundleManagementService bundleService,
                            BundleVersionManagementService versionService) {
        this.bundleService = bundleService;
        this.versionService = versionService;
    }

    // ========== Bundle ==========

    @PostMapping("/bundles")
    @Operation(summary = "创建 Bundle")
    public ApiResponse<BundleDto> createBundle(@RequestBody CreateBundleRequest request) {
        BundleDto dto = bundleService.create(request);
        return ApiResponse.success(dto, "Bundle 创建成功");
    }

    @GetMapping("/bundles")
    @Operation(summary = "查询全部 Bundle 列表")
    public ApiResponse<List<BundleDto>> listBundles() {
        List<BundleDto> list = bundleService.listAll();
        return ApiResponse.success(list);
    }

    @GetMapping("/bundles/{fqn}")
    @Operation(summary = "按 FQN 查询 Bundle")
    public ApiResponse<BundleDto> getBundle(@PathVariable String fqn) {
        Optional<BundleDto> dto = bundleService.findByFqn(fqn);
        return dto.map(b -> ApiResponse.success(b))
                .orElseGet(() -> ApiResponse.error(30102, "Bundle 不存在: " + fqn));
    }

    @DeleteMapping("/bundles/{fqn}")
    @Operation(summary = "删除 Bundle（isSystem=true 禁止删除）")
    public ApiResponse<Void> deleteBundle(@PathVariable String fqn) {
        bundleService.delete(fqn);
        return ApiResponse.success(null, "Bundle 删除成功");
    }

    // ========== BundleVersion ==========

    @PostMapping("/bundles/{bundleFqn}/versions")
    @Operation(summary = "从最新已发布版本创建草稿")
    public ApiResponse<BundleVersionDto> createDraft(
            @PathVariable String bundleFqn,
            @RequestBody CreateDraftRequest request) {
        request.setBundleFqn(bundleFqn);
        BundleVersionDto dto = versionService.createDraft(request);
        return ApiResponse.success(dto, "草稿版本创建成功");
    }

    @GetMapping("/bundles/{bundleFqn}/versions")
    @Operation(summary = "查询 Bundle 的所有版本")
    public ApiResponse<List<BundleVersionDto>> listVersions(
            @PathVariable String bundleFqn) {
        List<BundleVersionDto> list = versionService.listByBundle(bundleFqn);
        return ApiResponse.success(list);
    }

    @GetMapping("/versions/{fqn}")
    @Operation(summary = "按 FQN 查询版本")
    public ApiResponse<BundleVersionDto> getVersion(@PathVariable String fqn) {
        Optional<BundleVersionDto> dto = versionService.findByFqn(fqn);
        return dto.map(v -> ApiResponse.success(v))
                .orElseGet(() -> ApiResponse.error(30102, "版本不存在: " + fqn));
    }

    @PostMapping("/versions/{fqn}/publish")
    @Operation(summary = "发布草稿版本")
    public ApiResponse<BundleVersionDto> publishVersion(@PathVariable String fqn) {
        BundleVersionDto dto = versionService.publish(fqn);
        return ApiResponse.success(dto, "版本发布成功");
    }

    // ========== Bundle 依赖 ==========

    @PostMapping("/versions/{fqn}/dependencies")
    @Operation(summary = "声明跨 Bundle 依赖（精确版本）")
    public ApiResponse<Void> declareDependency(
            @PathVariable String fqn,
            @RequestBody Map<String, String> body) {
        String targetFqn = body.get("targetVersionFqn");
        versionService.declareDependency(fqn, targetFqn);
        return ApiResponse.success(null, "依赖声明成功: " + fqn + " → " + targetFqn);
    }
}
