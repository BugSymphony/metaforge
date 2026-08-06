package com.metaforge.metamodel.domain.service;

import com.metaforge.common.util.JsonbUtils;
import com.metaforge.metamodel.domain.model.aggregate.Bundle;
import com.metaforge.metamodel.domain.model.aggregate.BundleVersion;
import com.metaforge.metamodel.domain.model.entity.AttributeTemplate;
import com.metaforge.metamodel.domain.model.entity.EntitySchema;
import com.metaforge.metamodel.domain.model.entity.Package;
import com.metaforge.metamodel.domain.model.entity.RelationSchema;
import com.metaforge.metamodel.domain.repository.*;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 导出领域服务。
 * 支持 Bundle 全量导出和 Package 级导出到 YAML/JSON 格式，
 * 自动包含属性模板依赖。
 */
@Component
public class ExportService {

    private final BundleRepository bundleRepository;
    private final BundleVersionRepository versionRepository;
    private final PackageRepository packageRepository;
    private final EntitySchemaRepository entityRepository;
    private final RelationSchemaRepository relationRepository;
    private final AttributeTemplateRepository templateRepository;

    public ExportService(BundleRepository bundleRepository,
                          BundleVersionRepository versionRepository,
                          PackageRepository packageRepository,
                          EntitySchemaRepository entityRepository,
                          RelationSchemaRepository relationRepository,
                          AttributeTemplateRepository templateRepository) {
        this.bundleRepository = bundleRepository;
        this.versionRepository = versionRepository;
        this.packageRepository = packageRepository;
        this.entityRepository = entityRepository;
        this.relationRepository = relationRepository;
        this.templateRepository = templateRepository;
    }

    /**
     * Bundle 全量导出：包含所有 Package、EntitySchema、RelationSchema、AttributeTemplate。
     */
    public Map<String, Object> exportBundleFull(String bundleFqn) {
        Bundle bundle = bundleRepository.findByFqn(bundleFqn)
                .orElseThrow(() -> new NoSuchElementException("Bundle 不存在: " + bundleFqn));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("bundle", Map.of(
                "fqn", bundle.getFqnValue(),
                "name", bundle.getName(),
                "description", bundle.getDescription()
        ));

        List<BundleVersion> versions = versionRepository
                .findByBundleFqnOrderByCreatedTimeDesc(bundleFqn);
        List<Map<String, Object>> versionList = new ArrayList<>();
        String latestVersionFqn = null;

        for (BundleVersion bv : versions) {
            Map<String, Object> vMap = new LinkedHashMap<>();
            vMap.put("fqn", bv.getFqnValue());
            vMap.put("status", bv.getStatus().name());
            vMap.put("upgradeLevel", bv.getUpgradeLevel() != null
                    ? bv.getUpgradeLevel().name() : null);
            if (bv.isPublished() && latestVersionFqn == null) {
                latestVersionFqn = bv.getFqnValue();
            }
            versionList.add(vMap);
        }
        result.put("versions", versionList);

        if (latestVersionFqn != null) {
            result.put("packages", exportPackages(latestVersionFqn));
            result.put("entitySchemas", exportEntities(latestVersionFqn));
            result.put("relationSchemas", exportRelations(latestVersionFqn));
            result.put("attributeTemplates", exportAttributes(latestVersionFqn));
        }

        return result;
    }

    private List<Map<String, Object>> exportPackages(String versionFqn) {
        return packageRepository.findByBundleVersionFqn(versionFqn).stream()
                .map(p -> Map.<String, Object>of(
                        "fqn", p.getFqnValue(),
                        "parentPackageFqn", Objects.requireNonNullElse(p.getParentPackageFqn(), ""),
                        "depth", p.getDepth(),
                        "description", p.getDescription()
                )).toList();
    }

    private List<Map<String, Object>> exportEntities(String versionFqn) {
        return entityRepository.findByBundleVersionFqn(versionFqn).stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("fqn", e.getFqnValue());
                    m.put("name", e.getName());
                    m.put("description", e.getDescription());
                    m.put("packageFqn", e.getPackageFqn());
                    m.put("nativeAttributes", e.getNativeAttributes());
                    m.put("mountedTemplateFqns", e.getMountedTemplateFqns());
                    m.put("jsonSchema", e.getJsonSchema());
                    return m;
                }).toList();
    }

    private List<Map<String, Object>> exportRelations(String versionFqn) {
        return relationRepository.findByBundleVersionFqn(versionFqn).stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("fqn", e.getFqnValue());
                    m.put("name", e.getName());
                    m.put("description", e.getDescription());
                    m.put("sourceFqn", e.getSourceFqn());
                    m.put("targetFqn", e.getTargetFqn());
                    m.put("associationType", e.getAssociationType() != null
                            ? e.getAssociationType().name() : null);
                    m.put("cardinalitySource", e.getCardinalitySource() != null
                            ? e.getCardinalitySource().getNotation() : null);
                    m.put("cardinalityTarget", e.getCardinalityTarget() != null
                            ? e.getCardinalityTarget().getNotation() : null);
                    return m;
                }).toList();
    }

    private List<Map<String, Object>> exportAttributes(String versionFqn) {
        return templateRepository.findByBundleVersionFqn(versionFqn).stream()
                .map(e -> Map.<String, Object>of(
                        "fqn", e.getFqnValue(),
                        "name", e.getName(),
                        "description", Objects.requireNonNullElse(e.getDescription(), ""),
                        "attributeDefinitions", Objects.requireNonNullElse(
                                e.getAttributeDefinitions(), "")
                )).toList();
    }

    /**
     * 格式化为 YAML 或 JSON 字符串。
     */
    public String format(Map<String, Object> data, String format) {
        if ("YAML".equalsIgnoreCase(format)) {
            return toYaml(data);
        }
        return JsonbUtils.toJsonb(data);
    }

    private String toYaml(Map<String, Object> data) {
        StringBuilder sb = new StringBuilder();
        writeYaml(sb, data, 0);
        return sb.toString();
    }

    private void writeYaml(StringBuilder sb, Map<String, Object> data, int indent) {
        String pad = "  ".repeat(indent);
        for (var entry : data.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> m) {
                sb.append(pad).append(entry.getKey()).append(":\n");
                @SuppressWarnings("unchecked")
                Map<String, Object> subMap = (Map<String, Object>) m;
                writeYaml(sb, subMap, indent + 1);
            } else if (value instanceof List<?> list) {
                sb.append(pad).append(entry.getKey()).append(":\n");
                for (Object item : list) {
                    if (item instanceof Map<?, ?> itemMap) {
                        sb.append(pad).append("  - ");
                        @SuppressWarnings("unchecked")
                        Map<String, Object> subMap = (Map<String, Object>) itemMap;
                        writeInlineYaml(sb, subMap, indent + 1);
                    } else {
                        sb.append(pad).append("  - ").append(item).append("\n");
                    }
                }
            } else {
                sb.append(pad).append(entry.getKey()).append(": ").append(value).append("\n");
            }
        }
    }

    private void writeInlineYaml(StringBuilder sb, Map<String, Object> data, int indent) {
        sb.append("{");
        boolean first = true;
        for (var entry : data.entrySet()) {
            if (!first) sb.append(", ");
            sb.append(entry.getKey()).append(": ").append(entry.getValue());
            first = false;
        }
        sb.append("}\n");
    }
}
