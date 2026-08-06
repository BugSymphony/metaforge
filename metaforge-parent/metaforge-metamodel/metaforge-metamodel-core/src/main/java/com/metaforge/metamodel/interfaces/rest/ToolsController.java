package com.metaforge.metamodel.interfaces.rest;

import com.metaforge.common.dto.ApiResponse;
import com.metaforge.metamodel.api.enums.VersionStatus;
import com.metaforge.metamodel.domain.model.valueobject.FqnParts;
import com.metaforge.metamodel.domain.repository.BundleVersionRepository;
import com.metaforge.metamodel.domain.service.FqnGenerator;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 元模型工具类 REST 控制器。
 * 提供 FQN 解析等通用能力。
 */
@RestController
@RequestMapping("/api/v1/metamodel/tools")
@Tag(name = "metamodel-tools")
public class ToolsController {

    private final FqnGenerator fqnGenerator;
    private final BundleVersionRepository versionRepository;

    public ToolsController(FqnGenerator fqnGenerator,
                           BundleVersionRepository versionRepository) {
        this.fqnGenerator = fqnGenerator;
        this.versionRepository = versionRepository;
    }

    /**
     * 解析 FQN 为完整带版本格式。
     * 省略版本时自动解析为最新已发布版本。
     */
    @PostMapping("/resolve-fqn")
    @Operation(summary = "解析 FQN，省略版本时解析为最新已发布版本")
    public ApiResponse<Map<String, String>> resolveFqn(
            @RequestBody Map<String, String> body) {
        String fqn = body.get("fqn");
        String pureFqn = fqnGenerator.stripTypePrefix(fqn);
        FqnParts parts = fqnGenerator.parse(pureFqn);

        String resolvedFqn;
        if (parts.version() != null) {
            resolvedFqn = pureFqn;
        } else {
            // 版本省略语法：order.pkg_order.Order → bundleCode + path 段
            String bundleCode;
            String pathSuffix;
            int firstDot = pureFqn.indexOf('.');
            if (firstDot > 0) {
                bundleCode = pureFqn.substring(0, firstDot);
                pathSuffix = pureFqn.substring(firstDot);
            } else {
                bundleCode = parts.bundleCode();
                pathSuffix = "";
            }
            String version = resolveLatestVersion(bundleCode);
            if (version == null) {
                return ApiResponse.error(30102, "Bundle " + bundleCode + " 不存在已发布版本，无法解析 FQN: " + fqn);
            }
            resolvedFqn = bundleCode + ":" + version + pathSuffix;
        }

        return ApiResponse.success(Map.of("resolvedFqn", resolvedFqn), "FQN 解析成功");
    }

    /**
     * 查询 Bundle 的最新已发布版本号。
     */
    private String resolveLatestVersion(String bundleCode) {
        return versionRepository
                .findTopByBundleFqnAndStatusOrderByCreatedTimeDesc(bundleCode, VersionStatus.PUBLISHED)
                .map(v -> fqnGenerator.toVersion(v.getFqnValue()))
                .orElse(null);
    }
}
