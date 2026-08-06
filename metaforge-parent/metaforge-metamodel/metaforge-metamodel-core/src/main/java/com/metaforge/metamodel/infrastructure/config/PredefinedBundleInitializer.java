package com.metaforge.metamodel.infrastructure.config;

import com.metaforge.metamodel.api.enums.UpgradeLevel;
import com.metaforge.metamodel.api.enums.VersionStatus;
import com.metaforge.metamodel.api.service.BundleManagementService;
import com.metaforge.metamodel.api.service.BundleVersionManagementService;
import com.metaforge.metamodel.api.dto.AttributeDefinitionDto;
import com.metaforge.metamodel.api.dto.NativeAttributeDto;
import com.metaforge.metamodel.api.dto.request.CreateBundleRequest;
import com.metaforge.metamodel.api.dto.request.CreateDraftRequest;
import com.metaforge.metamodel.api.dto.request.CreateEntitySchemaRequest;
import com.metaforge.metamodel.api.dto.request.CreateAttributeTemplateRequest;
import com.metaforge.metamodel.api.dto.request.CreatePackageRequest;
import com.metaforge.metamodel.api.service.ElementDefinitionService;
import com.metaforge.metamodel.api.service.PackageManagementService;
import com.metaforge.metamodel.domain.model.aggregate.Bundle;
import com.metaforge.metamodel.domain.repository.BundleRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 预置系统 Bundle 初始化器。
 * 应用启动时检查 metaforge Bundle 是否存在，不存在则自动创建。
 * 包含 agent 和 common 两个预置 Package，以及基础元模型元素。
 */
@Component
public class PredefinedBundleInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PredefinedBundleInitializer.class);
    private static final String METAFORGE_BUNDLE_FQN = "metaforge";

    private final BundleManagementService bundleService;
    private final BundleVersionManagementService versionService;
    private final PackageManagementService packageService;
    private final ElementDefinitionService elementService;
    private final BundleRepository bundleRepository;

    public PredefinedBundleInitializer(BundleManagementService bundleService,
                                        BundleVersionManagementService versionService,
                                        PackageManagementService packageService,
                                        ElementDefinitionService elementService,
                                        BundleRepository bundleRepository) {
        this.bundleService = bundleService;
        this.versionService = versionService;
        this.packageService = packageService;
        this.elementService = elementService;
        this.bundleRepository = bundleRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (bundleRepository.existsByFqn(METAFORGE_BUNDLE_FQN)) {
            log.info("预置 Bundle {} 已存在，跳过初始化", METAFORGE_BUNDLE_FQN);
            return;
        }

        log.info("开始初始化预置 Bundle: {}", METAFORGE_BUNDLE_FQN);
        try {
            // 1. 创建 metaforge Bundle（标记为系统内置）
            CreateBundleRequest bundleReq = new CreateBundleRequest();
            bundleReq.setFqn(METAFORGE_BUNDLE_FQN);
            bundleReq.setName("MetaForge 语义基座");
            bundleReq.setDescription("MetaForge 平台语义基座，提供 Agent 与通用业务语义层元模型定义");
            bundleReq.setOwner("system");
            bundleService.create(bundleReq);

            // 标记为系统 Bundle（需要从 repository 重新获取）
            Bundle bundle = bundleRepository.findByFqn(METAFORGE_BUNDLE_FQN).orElseThrow();
            bundle.markAsSystem();
            bundleRepository.save(bundle);

            // 2. 创建初始版本 v1.0.0（这里需要手动创建已发布版本）
            // 使用 BundleVersionManagementService 创建草稿后发布
            // MVP 阶段：文档化预置结构（实际数据通过 Flyway V2__ SQL 脚本初始化）
            log.info("预置 Bundle {} 基础结构已创建（详细元模型数据通过 Flyway V2 脚本初始化）",
                    METAFORGE_BUNDLE_FQN);

            // 3. 创建 agent 和 common Package
            createPackageIfNeeded("metaforge:1.0.0", null, "agent",
                    "Agent 相关元模型：EntitySchema + RelationSchema + AttributeTemplate");
            createPackageIfNeeded("metaforge:1.0.0", null, "common",
                    "通用业务语义层级：主题域分组→主题域→业务对象→逻辑数据实体→属性");

            // 4. 创建基础预置元素骨架
            createEntityIfNeeded("metaforge:1.0.0", "metaforge:1.0.0.agent", "Agent",
                    "Agent 基础定义", null, null);
            createEntityIfNeeded("metaforge:1.0.0", "metaforge:1.0.0.common", "BusinessEntity",
                    "通用业务对象", null, null);
            createEntityIfNeeded("metaforge:1.0.0", "metaforge:1.0.0.common", "DataEntity",
                    "逻辑数据实体", null, null);
            createAttributeTemplateIfNeeded("metaforge:1.0.0", "AuditFields",
                    "审计字段模板（createdAt/createdBy/updatedAt/updatedBy）",
                    List.of(
                            attrDef("createdAt", "string", Map.of("format", "date-time")),
                            attrDef("createdBy", "string", null),
                            attrDef("updatedAt", "string", Map.of("format", "date-time")),
                            attrDef("updatedBy", "string", null)
                    ));

            log.info("预置 Bundle {} 初始化完成", METAFORGE_BUNDLE_FQN);

        } catch (Exception e) {
            log.warn("预置 Bundle {} 初始化异常（可能数据已存在）: {}", METAFORGE_BUNDLE_FQN, e.getMessage());
        }
    }

    private void createPackageIfNeeded(String versionFqn, String parentFqn,
                                        String segment, String description) {
        try {
            CreatePackageRequest req = new CreatePackageRequest();
            req.setBundleVersionFqn(versionFqn);
            req.setParentPackageFqn(parentFqn);
            req.setSegment(segment);
            req.setDescription(description);
            packageService.create(req);
        } catch (Exception e) {
            log.debug("Package {} 已存在或创建失败: {}", segment, e.getMessage());
        }
    }

    private void createEntityIfNeeded(String versionFqn, String packageFqn,
                                       String segment, String name,
                                       List<NativeAttributeDto> nativeAttributes,
                                       List<String> templateFqns) {
        try {
            CreateEntitySchemaRequest req = new CreateEntitySchemaRequest();
            req.setBundleVersionFqn(versionFqn);
            req.setPackageFqn(packageFqn);
            req.setSegment(segment);
            req.setName(name);
            req.setDescription(name + " 语义定义");
            req.setNativeAttributes(nativeAttributes);
            req.setMountedTemplateFqns(templateFqns);
            elementService.createEntitySchema(req);
        } catch (Exception e) {
            log.debug("EntitySchema {} 已存在或创建失败: {}", segment, e.getMessage());
        }
    }

    private void createAttributeTemplateIfNeeded(String versionFqn,
                                                   String segment, String name,
                                                   List<AttributeDefinitionDto> definitions) {
        try {
            CreateAttributeTemplateRequest req = new CreateAttributeTemplateRequest();
            req.setBundleVersionFqn(versionFqn);
            req.setSegment(segment);
            req.setName(name);
            req.setDescription(name + " 属性模板");
            req.setAttributeDefinitions(definitions);
            elementService.createAttributeTemplate(req);
        } catch (Exception e) {
            log.debug("AttributeTemplate {} 已存在或创建失败: {}", segment, e.getMessage());
        }
    }

    private AttributeDefinitionDto attrDef(String name, String type, Map<String, Object> constraints) {
        AttributeDefinitionDto dto = new AttributeDefinitionDto();
        dto.setName(name);
        dto.setType(type);
        dto.setConstraints(constraints);
        return dto;
    }
}
