package com.metaforge.metamodel.domain.service;

import com.metaforge.metamodel.api.enums.ElementType;
import com.metaforge.metamodel.domain.model.valueobject.FqnParts;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * FQN 生成器实现——纯字符串变换，无状态线程安全。
 * 解析为 best-effort，不进行格式校验（校验由上层写入/发布环节完成）。
 */
@Service
public class FqnGeneratorImpl implements FqnGenerator {

    /** 版本号正则：匹配 \d+.\d+.\d+ 格式 */
    private static final Pattern VERSION_PATTERN = Pattern.compile("^\\d+\\.\\d+\\.\\d+$");

    /** FQN 中版本号所在位置的正则：<bundle>:<version> */
    private static final Pattern FQN_VERSION_SEGMENT =
            Pattern.compile("^([a-z][a-z0-9_-]*):(\\d+\\.\\d+\\.\\d+)(.*)$");

    // ========== 生成 ==========

    @Override
    public String bundle(String code) {
        return code;
    }

    @Override
    public String bundleVersion(String code, String version) {
        return code + ":" + version;
    }

    @Override
    public String package_(String parentFqn, String segment) {
        return parentFqn + "." + segment;
    }

    @Override
    public String entitySchema(String packageFqn, String segment) {
        return packageFqn + "." + segment;
    }

    @Override
    public String relationSchema(String packageFqn, String segment) {
        return packageFqn + "." + segment;
    }

    @Override
    public String attributeTemplate(String bundleVersionFqn, String segment) {
        return bundleVersionFqn + "." + segment;
    }

    // ========== 解析 ==========

    @Override
    public FqnParts parse(String fqn) {
        if (fqn == null || fqn.isBlank()) {
            throw new IllegalArgumentException("FQN 不能为空");
        }

        String trimmed = fqn.trim();

        // 先剥离类型前缀
        String pureFqn = stripTypePrefix(trimmed);

        // 提取 bundle code
        int colonIdx = pureFqn.indexOf(':');
        if (colonIdx < 0) {
            return FqnParts.forBundle(pureFqn);
        }

        String bundleCode = pureFqn.substring(0, colonIdx);
        String remainder = pureFqn.substring(colonIdx + 1);

        // 提取版本号（: 后前三个数字 segment）
        int firstDot = remainder.indexOf('.');
        if (firstDot < 0) {
            // 仅有版本号，无路径段
            return FqnParts.forBundleVersion(bundleCode, remainder);
        }

        int secondDot = remainder.indexOf('.', firstDot + 1);
        if (secondDot < 0) {
            // remainder 不是完整版本号格式
            return FqnParts.forBundleVersion(bundleCode, remainder);
        }

        String maybeVersion = remainder.substring(0, remainder.indexOf('.', secondDot + 1) > 0
                ? remainder.indexOf('.', secondDot + 1)
                : remainder.length());

        // 取前三个点分隔的数字
        String[] parts = remainder.split("\\.", 4);
        if (parts.length >= 3 && VERSION_PATTERN.matcher(
                parts[0] + "." + parts[1] + "." + parts[2]).matches()) {
            String version = parts[0] + "." + parts[1] + "." + parts[2];
            String afterVersion = remainder.substring(version.length());

            List<String> segments = new ArrayList<>();
            String shortName;
            String parentFqn;

            if (afterVersion.isEmpty()) {
                shortName = version;
                parentFqn = bundleCode;
                segments = Collections.emptyList();
            } else if (afterVersion.startsWith(".")) {
                afterVersion = afterVersion.substring(1);
                if (afterVersion.isEmpty()) {
                    shortName = version;
                    parentFqn = bundleCode;
                } else {
                    String[] segs = afterVersion.split("\\.");
                    // 最后一段为短名（实体/关系/模板名），其余为 Package 路径段
                    for (int i = 0; i < segs.length - 1; i++) {
                        segments.add(segs[i]);
                    }
                    shortName = segs[segs.length - 1];
                    parentFqn = bundleCode + ":" + version;
                    if (segs.length > 1) {
                        for (int i = 0; i < segs.length - 1; i++) {
                            if (i == 0) {
                                parentFqn += ".";
                            }
                            parentFqn += segs[i];
                            if (i < segs.length - 2) {
                                parentFqn += ".";
                            }
                        }
                    }
                }
            } else {
                shortName = parts[parts.length - 1];
                parentFqn = bundleCode + ":" + version;
            }

            return new FqnParts(bundleCode, version, segments, shortName, parentFqn);
        }

        // 无法解析版本号，简单处理
        return FqnParts.forBundle(bundleCode);
    }

    @Override
    public String toParentFqn(String fqn) {
        int lastDot = fqn.lastIndexOf('.');
        if (lastDot < 0) {
            return fqn.substring(0, fqn.indexOf(':') >= 0 ? fqn.indexOf(':') + 1 : fqn.length());
        }
        return fqn.substring(0, lastDot);
    }

    @Override
    public String toShortName(String fqn) {
        int lastDot = fqn.lastIndexOf('.');
        if (lastDot < 0) {
            int colonIdx = fqn.indexOf(':');
            if (colonIdx >= 0) {
                return fqn.substring(colonIdx + 1);
            }
            return fqn;
        }
        return fqn.substring(lastDot + 1);
    }

    @Override
    public String toBundleCode(String fqn) {
        int colonIdx = fqn.indexOf(':');
        if (colonIdx < 0) {
            return fqn;
        }
        return fqn.substring(0, colonIdx);
    }

    @Override
    public String toVersion(String fqn) {
        var matcher = FQN_VERSION_SEGMENT.matcher(fqn);
        if (matcher.matches()) {
            return matcher.group(2);
        }
        return null;
    }

    @Override
    public String toFilePath(String fqn) {
        if (fqn == null) return null;
        int colonIdx = fqn.indexOf(':');
        if (colonIdx < 0) {
            return fqn + ".json";
        }
        String bundleCode = fqn.substring(0, colonIdx);
        String rest = fqn.substring(colonIdx + 1);
        String[] parts = rest.split("\\.", 4);
        String version = parts[0] + "." + parts[1] + "." + parts[2];
        String path = bundleCode + "/" + version;
        if (parts.length > 3 && !parts[3].isEmpty()) {
            path += "/" + parts[3].replace(".", "/");
        }
        return path + ".json";
    }

    // ========== 类型前缀 ==========

    @Override
    public String stripTypePrefix(String typedFqn) {
        if (typedFqn == null) return null;
        int colonIdx = typedFqn.indexOf(':');
        if (colonIdx < 0) return typedFqn;

        String prefix = typedFqn.substring(0, colonIdx);
        ElementType type = ElementType.fromPrefix(prefix);
        if (type != null) {
            return typedFqn.substring(colonIdx + 1);
        }
        return typedFqn;
    }

    @Override
    public ElementType detectType(String typedFqn) {
        if (typedFqn == null) return null;
        int colonIdx = typedFqn.indexOf(':');
        if (colonIdx < 0) return null;

        String prefix = typedFqn.substring(0, colonIdx);
        return ElementType.fromPrefix(prefix);
    }
}
