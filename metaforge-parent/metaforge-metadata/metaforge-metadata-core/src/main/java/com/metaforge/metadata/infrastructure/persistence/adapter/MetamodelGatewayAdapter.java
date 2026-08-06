package com.metaforge.metadata.infrastructure.persistence.adapter;

import com.metaforge.common.util.JsonbUtils;
import com.metaforge.metadata.domain.repository.EntitySchemaRepository;
import com.metaforge.metadata.infrastructure.persistence.jpa.MetamodelBundleVersionJpo;
import com.metaforge.metadata.infrastructure.persistence.jpa.MetamodelBundleVersionJpaRepository;
import com.metaforge.metadata.infrastructure.persistence.jpa.MetamodelEntitySchemaJpo;
import com.metaforge.metadata.infrastructure.persistence.jpa.MetamodelEntitySchemaJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 上游 metamodel-governance BC 的网关适配器（跨 Schema 只读）。
 * <p>直接读取同库 metamodel_governance 的 entity_schema / bundle_version 表，
 * 为 metadata BC 提供已发布 EntitySchema 的 JSON Schema 与发布状态。</p>
 */
@Component
public class MetamodelGatewayAdapter implements EntitySchemaRepository {

    private static final Logger log = LoggerFactory.getLogger(MetamodelGatewayAdapter.class);

    private static final String STATUS_PUBLISHED = "PUBLISHED";

    private final MetamodelEntitySchemaJpaRepository entitySchemaJpaRepository;
    private final MetamodelBundleVersionJpaRepository bundleVersionJpaRepository;

    public MetamodelGatewayAdapter(MetamodelEntitySchemaJpaRepository entitySchemaJpaRepository,
                                   MetamodelBundleVersionJpaRepository bundleVersionJpaRepository) {
        this.entitySchemaJpaRepository = entitySchemaJpaRepository;
        this.bundleVersionJpaRepository = bundleVersionJpaRepository;
    }

    @Override
    public Optional<Map<String, Object>> getJsonSchema(String entitySchemaFqn) {
        log.debug("获取 EntitySchema JSON Schema: {}", entitySchemaFqn);
        if (entitySchemaFqn == null || entitySchemaFqn.isBlank()) {
            return Optional.empty();
        }
        Optional<MetamodelEntitySchemaJpo> schema = entitySchemaJpaRepository
                .findByFqn(resolveFqn(entitySchemaFqn));
        if (schema.isEmpty()) {
            return Optional.empty();
        }
        String jsonSchema = schema.get().getJsonSchema();
        if (jsonSchema == null || jsonSchema.isBlank()) {
            return Optional.empty();
        }
        try {
            Map<String, Object> parsed = JsonbUtils.fromJsonb(jsonSchema, Map.class);
            return parsed == null || parsed.isEmpty() ? Optional.empty() : Optional.of(parsed);
        } catch (Exception e) {
            log.warn("解析 EntitySchema JSON Schema 失败: {} — {}", entitySchemaFqn, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public boolean existsByFqn(String entitySchemaFqn) {
        log.debug("检查 EntitySchema 是否存在: {}", entitySchemaFqn);
        if (entitySchemaFqn == null || entitySchemaFqn.isBlank()) {
            return false;
        }
        return entitySchemaJpaRepository.existsByFqn(resolveFqn(entitySchemaFqn));
    }

    @Override
    public boolean isPublished(String entitySchemaFqn) {
        log.debug("检查 EntitySchema 是否已发布: {}", entitySchemaFqn);
        if (entitySchemaFqn == null || entitySchemaFqn.isBlank()) {
            return false;
        }
        Optional<MetamodelEntitySchemaJpo> schema = entitySchemaJpaRepository
                .findByFqn(resolveFqn(entitySchemaFqn));
        if (schema.isEmpty()) {
            return false;
        }
        return bundleVersionJpaRepository.findByFqn(schema.get().getBundleVersionFqn())
                .map(v -> STATUS_PUBLISHED.equals(v.getStatus()))
                .orElse(false);
    }

    /**
     * 解析 EntitySchema FQN，支持版本省略语法。
     * <p>精确匹配优先；若 FQN 未携带版本段（如 order.pkg_order.Order），
     * 则解析 bundle-code 并取该 bundle 最新已发布版本补齐。</p>
     */
    private String resolveFqn(String entitySchemaFqn) {
        if (entitySchemaJpaRepository.existsByFqn(entitySchemaFqn)) {
            return entitySchemaFqn;
        }
        if (entitySchemaFqn.indexOf(':') >= 0) {
            return entitySchemaFqn;
        }
        int firstDot = entitySchemaFqn.indexOf('.');
        String bundleCode = firstDot > 0 ? entitySchemaFqn.substring(0, firstDot) : entitySchemaFqn;
        if (bundleCode.isBlank()) {
            return entitySchemaFqn;
        }
        List<MetamodelBundleVersionJpo> published = bundleVersionJpaRepository
                .findByBundleFqnAndStatusOrderByCreatedTimeDesc(bundleCode, STATUS_PUBLISHED);
        if (published.isEmpty()) {
            return entitySchemaFqn;
        }
        String candidate = published.get(0).getFqn() + entitySchemaFqn.substring(bundleCode.length());
        log.debug("版本省略 FQN 解析: {} → {}", entitySchemaFqn, candidate);
        return candidate;
    }
}
