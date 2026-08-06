package com.metaforge.metamodel.domain.model.valueobject;

import java.util.Collections;
import java.util.List;

/**
 * FQN 解析结果的不可变记录。
 * 调用 FqnGenerator.parse(fqn) 返回此对象。
 */
public record FqnParts(
        String bundleCode,
        String version,
        List<String> segments,
        String shortName,
        String parentFqn
) {
    public FqnParts {
        segments = segments != null
                ? Collections.unmodifiableList(segments)
                : Collections.emptyList();
    }

    /**
     * 创建仅包含 bundleCode 的 FqnParts（Bundle 级 FQN）。
     */
    public static FqnParts forBundle(String bundleCode) {
        return new FqnParts(bundleCode, null, Collections.emptyList(), bundleCode, null);
    }

    /**
     * 创建包含 bundleCode + version 的 FqnParts（BundleVersion 级 FQN）。
     */
    public static FqnParts forBundleVersion(String bundleCode, String version) {
        String fqn = bundleCode + ":" + version;
        return new FqnParts(bundleCode, version, Collections.emptyList(), version, bundleCode);
    }
}
