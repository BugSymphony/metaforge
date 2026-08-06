package com.metaforge.metamodel.application.service;

import com.metaforge.metamodel.api.dto.request.CreateDraftRequest;
import com.metaforge.metamodel.api.dto.response.BundleVersionDto;
import com.metaforge.metamodel.api.enums.UpgradeLevel;
import com.metaforge.metamodel.api.enums.VersionStatus;
import com.metaforge.metamodel.api.service.BundleVersionManagementService;
import com.metaforge.metamodel.domain.exception.FqnDuplicateException;
import com.metaforge.metamodel.domain.exception.FqnNotFoundException;
import com.metaforge.metamodel.domain.exception.VersionNotDraftException;
import com.metaforge.metamodel.domain.model.aggregate.BundleVersion;
import com.metaforge.metamodel.domain.model.aggregate.ExportManifest;
import com.metaforge.metamodel.domain.model.entity.AttributeTemplate;
import com.metaforge.metamodel.domain.model.entity.EntitySchema;
import com.metaforge.metamodel.domain.model.entity.Package;
import com.metaforge.metamodel.domain.model.entity.RelationSchema;
import com.metaforge.metamodel.domain.repository.AttributeTemplateRepository;
import com.metaforge.metamodel.domain.repository.BundleVersionRepository;
import com.metaforge.metamodel.domain.repository.EntitySchemaRepository;
import com.metaforge.metamodel.domain.repository.ExportManifestRepository;
import com.metaforge.metamodel.domain.repository.PackageRepository;
import com.metaforge.metamodel.domain.repository.RelationSchemaRepository;
import com.metaforge.metamodel.domain.service.AttributeMergeService;
import com.metaforge.metamodel.domain.service.BundleDependencyService;
import com.metaforge.metamodel.domain.service.FqnGenerator;
import com.metaforge.metamodel.domain.service.JsonSchemaCompiler;
import com.metaforge.metamodel.domain.service.UpgradeLevelValidator;
import com.metaforge.metamodel.domain.model.valueobject.Fqn;
import com.metaforge.metamodel.domain.model.valueobject.SemanticVersion;

import com.metaforge.common.util.JsonbUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * BundleVersion 管理应用服务实现。
 */
@Service
@Transactional
public class BundleVersionManagementServiceImpl implements BundleVersionManagementService {

    private static final Logger log = LoggerFactory.getLogger(BundleVersionManagementServiceImpl.class);

    private final BundleVersionRepository versionRepository;
    private final EntitySchemaRepository entityRepository;
    private final RelationSchemaRepository relationRepository;
    private final AttributeTemplateRepository templateRepository;
    private final PackageRepository packageRepository;
    private final ExportManifestRepository manifestRepository;
    private final FqnGenerator fqnGenerator;
    private final UpgradeLevelValidator upgradeLevelValidator;
    private final BundleDependencyService dependencyService;
    private final AttributeMergeService mergeService;
    private final JsonSchemaCompiler schemaCompiler;
    private final com.metaforge.metamodel.infrastructure.config.MetamodelProperties properties;

    public BundleVersionManagementServiceImpl(BundleVersionRepository versionRepository,
                                               EntitySchemaRepository entityRepository,
                                               RelationSchemaRepository relationRepository,
                                               AttributeTemplateRepository templateRepository,
                                               PackageRepository packageRepository,
                                               ExportManifestRepository manifestRepository,
                                               FqnGenerator fqnGenerator,
                                               UpgradeLevelValidator upgradeLevelValidator,
                                               BundleDependencyService dependencyService,
                                               AttributeMergeService mergeService,
                                               JsonSchemaCompiler schemaCompiler,
                                               com.metaforge.metamodel.infrastructure.config.MetamodelProperties properties) {
        this.versionRepository = versionRepository;
        this.entityRepository = entityRepository;
        this.relationRepository = relationRepository;
        this.templateRepository = templateRepository;
        this.packageRepository = packageRepository;
        this.manifestRepository = manifestRepository;
        this.fqnGenerator = fqnGenerator;
        this.upgradeLevelValidator = upgradeLevelValidator;
        this.dependencyService = dependencyService;
        this.mergeService = mergeService;
        this.schemaCompiler = schemaCompiler;
        this.properties = properties;
    }

    @Override
    public BundleVersionDto createDraft(CreateDraftRequest request) {
        String bundleFqn = request.getBundleFqn();
        UpgradeLevel level = parseUpgradeLevel(request.getUpgradeLevel());

        // 检查是否已存在草稿
        if (versionRepository.existsByBundleFqnAndStatus(bundleFqn, VersionStatus.DRAFT)) {
            throw new VersionNotDraftException(bundleFqn,
                    "Bundle " + bundleFqn + " 已存在草稿版本，不允许创建多个草稿");
        }

        // 查找最新已发布版本
        BundleVersion latestPublished = versionRepository
                .findTopByBundleFqnAndStatusOrderByCreatedTimeDesc(bundleFqn, VersionStatus.PUBLISHED)
                .orElseThrow(() -> new FqnNotFoundException(
                        "Bundle " + bundleFqn + " 不存在已发布版本，无法创建草稿"));

        // 从源版本创建草稿
        BundleVersion draft = BundleVersion.createDraftFrom(fqnGenerator, latestPublished, level);

        // 检查 FQN 唯一性
        if (versionRepository.findByFqn(draft.getFqnValue()).isPresent()) {
            throw new FqnDuplicateException(draft.getFqnValue());
        }

        BundleVersion saved = versionRepository.save(draft);

        // FR-020: 全量原子复制源版本内容到新草稿
        copyVersionContent(latestPublished.getFqnValue(), saved.getFqnValue());

        return toDto(saved);
    }

    /**
     * FR-020: 将源版本的内容（Package/EntitySchema/RelationSchema/AttributeTemplate/ExportManifest）
     * 复制到目标草稿版本。复制时 FQN 前缀从源版本号替换为目标版本号。
     */
    private void copyVersionContent(String sourceVersionFqn, String targetVersionFqn) {
        // Package
        for (Package src : packageRepository.findByBundleVersionFqn(sourceVersionFqn)) {
            int parentDepth = src.getParentPackageFqn() != null
                    ? packageRepository.findByFqn(src.getParentPackageFqn()).map(Package::getDepth).orElse(-1)
                    : -1;
            Package copy = Package.create(
                    Fqn.of(rekeyFqn(src.getFqnValue(), sourceVersionFqn, targetVersionFqn)),
                    targetVersionFqn,
                    src.getParentPackageFqn() != null
                            ? rekeyFqn(src.getParentPackageFqn(), sourceVersionFqn, targetVersionFqn)
                            : null,
                    src.getDescription(),
                    parentDepth,
                    properties.getMaxPackageDepth());
            copy.setEmbedding(src.getEmbedding());
            packageRepository.save(copy);
        }

        // EntitySchema
        for (EntitySchema src : entityRepository.findByBundleVersionFqn(sourceVersionFqn)) {
            EntitySchema copy = EntitySchema.create(
                    Fqn.of(rekeyFqn(src.getFqnValue(), sourceVersionFqn, targetVersionFqn)),
                    rekeyFqn(src.getPackageFqn(), sourceVersionFqn, targetVersionFqn),
                    targetVersionFqn,
                    src.getName(),
                    src.getDescription());
            copy.setNativeAttributes(src.getNativeAttributes());
            copy.setMountedTemplateFqns(rekeyTemplateList(src.getMountedTemplateFqns(),
                    sourceVersionFqn, targetVersionFqn));
            copy.setEmbedding(src.getEmbedding());
            entityRepository.save(copy);
        }

        // RelationSchema
        for (RelationSchema src : relationRepository.findByBundleVersionFqn(sourceVersionFqn)) {
            RelationSchema copy = RelationSchema.create(
                    Fqn.of(rekeyFqn(src.getFqnValue(), sourceVersionFqn, targetVersionFqn)),
                    rekeyFqn(src.getPackageFqn(), sourceVersionFqn, targetVersionFqn),
                    targetVersionFqn,
                    src.getName(),
                    src.getDescription(),
                    rekeyFqn(src.getSourceFqn(), sourceVersionFqn, targetVersionFqn),
                    rekeyFqn(src.getTargetFqn(), sourceVersionFqn, targetVersionFqn),
                    src.getAssociationType(),
                    src.getCardinalitySource(),
                    src.getCardinalityTarget());
            copy.setNativeAttributes(src.getNativeAttributes());
            copy.setMountedTemplateFqns(rekeyTemplateList(src.getMountedTemplateFqns(),
                    sourceVersionFqn, targetVersionFqn));
            copy.setEmbedding(src.getEmbedding());
            relationRepository.save(copy);
        }

        // AttributeTemplate
        for (AttributeTemplate src : templateRepository.findByBundleVersionFqn(sourceVersionFqn)) {
            AttributeTemplate copy = AttributeTemplate.create(
                    Fqn.of(rekeyFqn(src.getFqnValue(), sourceVersionFqn, targetVersionFqn)),
                    targetVersionFqn,
                    src.getName(),
                    src.getDescription());
            copy.setAttributeDefinitions(src.getAttributeDefinitions());
            templateRepository.save(copy);
        }

        // ExportManifest
        manifestRepository.findByBundleVersionFqn(sourceVersionFqn).ifPresent(src -> {
            List<String> rekeyed = src.getExportedPackageFqns() == null ? List.of()
                    : src.getExportedPackageFqns().stream()
                    .map(f -> rekeyFqn(f, sourceVersionFqn, targetVersionFqn))
                    .toList();
            manifestRepository.save(ExportManifest.create(targetVersionFqn, rekeyed));
        });

        log.info("FR-020 草稿复制完成: {} → {}", sourceVersionFqn, targetVersionFqn);
    }

    /**
     * 将 FQN 中的源版本号前缀替换为目标版本号前缀。
     */
    private String rekeyFqn(String fqn, String sourceVersionFqn, String targetVersionFqn) {
        if (fqn == null) {
            return null;
        }
        String prefix = sourceVersionFqn + ".";
        if (fqn.equals(sourceVersionFqn)) {
            return targetVersionFqn;
        }
        if (fqn.startsWith(prefix)) {
            return targetVersionFqn + fqn.substring(sourceVersionFqn.length());
        }
        return fqn;
    }

    /**
     * 重键 JSONB 挂载模板 FQN 列表。
     */
    private String rekeyTemplateList(String mountedJson, String sourceVersionFqn, String targetVersionFqn) {
        if (mountedJson == null || mountedJson.isBlank()) {
            return mountedJson;
        }
        List<String> fqns = JsonbUtils.fromJsonbList(mountedJson, String.class);
        List<String> rekeyed = fqns.stream()
                .map(f -> rekeyFqn(f, sourceVersionFqn, targetVersionFqn))
                .toList();
        return JsonbUtils.toJsonb(rekeyed);
    }

    /**
     * FR-022: 构建变更报告。
     * 对比源版本与草稿版本的元素集合（按 FQN 尾部段归一化），检测元素新增/删除。
     */
    private UpgradeLevelValidator.ChangeReport buildChangeReport(
            String sourceVersionFqn, String draftVersionFqn) {
        var sourceEntities = elementKeys(entityRepository.findByBundleVersionFqn(sourceVersionFqn)
                .stream().map(EntitySchema::getFqnValue).toList());
        var draftEntities = elementKeys(entityRepository.findByBundleVersionFqn(draftVersionFqn)
                .stream().map(EntitySchema::getFqnValue).toList());
        var sourceRels = elementKeys(relationRepository.findByBundleVersionFqn(sourceVersionFqn)
                .stream().map(RelationSchema::getFqnValue).toList());
        var draftRels = elementKeys(relationRepository.findByBundleVersionFqn(draftVersionFqn)
                .stream().map(RelationSchema::getFqnValue).toList());
        var sourceTpls = elementKeys(templateRepository.findByBundleVersionFqn(sourceVersionFqn)
                .stream().map(AttributeTemplate::getFqnValue).toList());
        var draftTpls = elementKeys(templateRepository.findByBundleVersionFqn(draftVersionFqn)
                .stream().map(AttributeTemplate::getFqnValue).toList());

        boolean hasAddition = hasNew(draftEntities, sourceEntities)
                || hasNew(draftRels, sourceRels)
                || hasNew(draftTpls, sourceTpls);
        boolean hasDeletion = hasNew(sourceEntities, draftEntities)
                || hasNew(sourceRels, draftRels)
                || hasNew(sourceTpls, draftTpls);

        return new UpgradeLevelValidator.ChangeReport(hasAddition, hasDeletion, false);
    }

    /**
     * 提取元素 FQN key，去除版本号前缀，仅保留版本后的路径段（用于跨版本归一化比较）。
     */
    private java.util.Set<String> elementKeys(List<String> fqns) {
        java.util.Set<String> keys = new java.util.HashSet<>();
        for (String fqn : fqns) {
            if (fqn == null) {
                continue;
            }
            int colonIdx = fqn.indexOf(':');
            if (colonIdx >= 0) {
                int dotIdx = fqn.indexOf('.', colonIdx);
                if (dotIdx >= 0) {
                    keys.add(fqn.substring(dotIdx));
                } else {
                    keys.add(":" + fqn.substring(colonIdx + 1));
                }
            } else {
                keys.add(fqn);
            }
        }
        return keys;
    }

    private boolean hasNew(java.util.Set<String> current, java.util.Set<String> baseline) {
        for (String key : current) {
            if (!baseline.contains(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public BundleVersionDto publish(String versionFqn) {
        BundleVersion version = versionRepository.findByFqn(versionFqn)
                .orElseThrow(() -> new FqnNotFoundException(versionFqn));

        if (version.getStatus() != VersionStatus.DRAFT) {
            throw new VersionNotDraftException(versionFqn,
                    version.getStatus().name());
        }

        // 发布前校验升级等级（对比源版本与草稿版本的实际变更）
        if (version.getUpgradeLevel() != null && version.getSourceVersionFqn() != null) {
            BundleVersion sourceVersion = versionRepository
                    .findByFqn(version.getSourceVersionFqn())
                    .orElse(null);
            if (sourceVersion != null) {
                upgradeLevelValidator.validate(sourceVersion, version,
                        version.getUpgradeLevel(),
                        buildChangeReport(sourceVersion.getFqnValue(), versionFqn));
            }
        }

        // 发布前校验依赖循环
        dependencyService.validateBeforePublish(versionFqn);

        // 执行发布：状态 DRAFT → PUBLISHED
        version.publish();
        BundleVersion saved = versionRepository.save(version);

        // 发布时属性平铺合并 + JSON Schema 编译固化
        // 根据宪法 VI：发布时自动合并原生属性与挂载模板组，生成扁平 JSON Schema
        compileJsonSchemasForVersion(saved.getFqnValue());

        return toDto(saved);
    }

    /**
     * 发布时对当前版本下所有 EntitySchema 和 RelationSchema 执行
     * 属性平铺合并 + JSON Schema 编译，结果写入对应实体的 json_schema 字段。
     */
    private void compileJsonSchemasForVersion(String versionFqn) {
        // EntitySchema
        List<EntitySchema> entities = entityRepository.findByBundleVersionFqn(versionFqn);
        for (EntitySchema entity : entities) {
            try {
                List<String> templateFqns = entity.getMountedTemplateFqns() != null
                        ? JsonbUtils.fromJsonbList(entity.getMountedTemplateFqns(), String.class)
                        : List.of();
                String merged = mergeService.merge(entity.getNativeAttributes(), templateFqns);
                String jsonSchema = schemaCompiler.compile(merged, entity.getName());
                entity.setJsonSchema(jsonSchema);
                entityRepository.save(entity);
                log.debug("EntitySchema {} JSON Schema 编译完成", entity.getFqnValue());
            } catch (Exception e) {
                log.error("EntitySchema {} JSON Schema 编译失败: {}", entity.getFqnValue(), e.getMessage());
                throw new RuntimeException("发布失败: EntitySchema " + entity.getFqnValue()
                        + " JSON Schema 编译异常 — " + e.getMessage(), e);
            }
        }

        // RelationSchema
        List<RelationSchema> relations = relationRepository.findByBundleVersionFqn(versionFqn);
        for (RelationSchema rel : relations) {
            try {
                List<String> templateFqns = rel.getMountedTemplateFqns() != null
                        ? JsonbUtils.fromJsonbList(rel.getMountedTemplateFqns(), String.class)
                        : List.of();
                String merged = mergeService.merge(rel.getNativeAttributes(), templateFqns);
                String jsonSchema = schemaCompiler.compile(merged, rel.getName());
                rel.setJsonSchema(jsonSchema);
                relationRepository.save(rel);
                log.debug("RelationSchema {} JSON Schema 编译完成", rel.getFqnValue());
            } catch (Exception e) {
                log.error("RelationSchema {} JSON Schema 编译失败: {}", rel.getFqnValue(), e.getMessage());
                throw new RuntimeException("发布失败: RelationSchema " + rel.getFqnValue()
                        + " JSON Schema 编译异常 — " + e.getMessage(), e);
            }
        }

        log.info("BundleVersion {} 发布完成 — 共编译 {} EntitySchema + {} RelationSchema",
                versionFqn, entities.size(), relations.size());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BundleVersionDto> findByFqn(String fqn) {
        return versionRepository.findByFqn(fqn).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BundleVersionDto> listByBundle(String bundleFqn) {
        return versionRepository.findByBundleFqnOrderByCreatedTimeDesc(bundleFqn).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public void declareDependency(String sourceVersionFqn, String targetVersionFqn) {
        dependencyService.declareDependency(sourceVersionFqn, targetVersionFqn);
    }

    private BundleVersionDto toDto(BundleVersion version) {
        BundleVersionDto dto = new BundleVersionDto();
        dto.setFqn(version.getFqnValue());
        dto.setBundleFqn(version.getBundleFqn());
        dto.setStatus(version.getStatus().name());
        dto.setSourceVersionFqn(version.getSourceVersionFqn());
        dto.setUpgradeLevel(version.getUpgradeLevel() != null
                ? version.getUpgradeLevel().name() : null);
        dto.setEnabled(version.deriveEnabled());
        dto.setCreatedTime(version.getCreatedTime());
        dto.setUpdatedTime(version.getUpdatedTime());
        return dto;
    }

    private UpgradeLevel parseUpgradeLevel(String level) {
        if (level == null || level.isBlank()) {
            return UpgradeLevel.PATCH;
        }
        return UpgradeLevel.valueOf(level.toUpperCase());
    }
}
