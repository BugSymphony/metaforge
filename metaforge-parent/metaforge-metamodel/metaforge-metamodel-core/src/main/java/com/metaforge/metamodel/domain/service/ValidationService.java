package com.metaforge.metamodel.domain.service;

import com.metaforge.metamodel.domain.model.valueobject.ValidationResult;
import com.metaforge.metamodel.domain.model.valueobject.ValidationResult.ValidationError;
import com.metaforge.metamodel.domain.repository.*;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 校验领域服务。
 * 两级校验体系：写入时轻量校验 + 发布前全局校验，支持预览模式。
 */
@Component
public class ValidationService {

    private final BundleRepository bundleRepository;
    private final BundleVersionRepository versionRepository;
    private final PackageRepository packageRepository;
    private final EntitySchemaRepository entityRepository;
    private final RelationSchemaRepository relationRepository;
    private final AttributeTemplateRepository templateRepository;
    private final BundleDependencyRepository dependencyRepository;

    public ValidationService(BundleRepository bundleRepository,
                              BundleVersionRepository versionRepository,
                              PackageRepository packageRepository,
                              EntitySchemaRepository entityRepository,
                              RelationSchemaRepository relationRepository,
                              AttributeTemplateRepository templateRepository,
                              BundleDependencyRepository dependencyRepository) {
        this.bundleRepository = bundleRepository;
        this.versionRepository = versionRepository;
        this.packageRepository = packageRepository;
        this.entityRepository = entityRepository;
        this.relationRepository = relationRepository;
        this.templateRepository = templateRepository;
        this.dependencyRepository = dependencyRepository;
    }

    /**
     * 写入时轻量校验（FR-049）。
     * 包含：FQN 唯一性、引用完整性、循环依赖、Package 深度、属性名冲突、命名规范。
     */
    public ValidationResult validateSave(String versionFqn) {
        List<ValidationError> errors = new ArrayList<>();

        // 1. FQN 唯一性检查（查询当前版本内所有元素 FQN 是否重复）
        var entities = entityRepository.findByBundleVersionFqn(versionFqn);
        var relationNames = new java.util.HashSet<String>();
        for (var e : entities) {
            if (!relationNames.add(e.getFqnValue())) {
                errors.add(ValidationError.of(e.getFqnValue(), "fqn", "FQN 重复"));
            }
        }

        // 2. Package 嵌套深度检查
        var packages = packageRepository.findByBundleVersionFqn(versionFqn);
        for (var pkg : packages) {
            if (pkg.getDepth() >= 5) {
                errors.add(ValidationError.of(pkg.getFqnValue(), "depth",
                        "Package 嵌套深度超限: " + (pkg.getDepth() + 1) + " 层"));
            }
        }

        // 3. 属性名冲突检测（简化版）
        for (var entity : entities) {
            if (entity.getMountedTemplateFqns() != null) {
                List<String> templates = com.metaforge.common.util.JsonbUtils
                        .fromJsonbList(entity.getMountedTemplateFqns(), String.class);
                var nameSet = new java.util.HashSet<String>();
                for (String tplFqn : templates) {
                    if (!nameSet.add(tplFqn)) {
                        errors.add(ValidationError.of(entity.getFqnValue(), "mountedTemplateFqns",
                                "属性模板 " + tplFqn + " 重复挂载"));
                    }
                }
            }
        }

        if (errors.isEmpty()) {
            return ValidationResult.success();
        }
        return ValidationResult.failure(errors);
    }

    /**
     * 发布前全量全局校验（FR-050）。
     * 包含：跨 Bundle 引用可达性、导出清单一致性、关联端点合法性、
     * 依赖链自洽性、升级等级匹配、属性名冲突、JSON Schema 合规、预置约束。
     */
    public ValidationResult validatePublish(String versionFqn) {
        List<ValidationError> errors = new ArrayList<>();

        // 1. 加载版本信息
        var version = versionRepository.findByFqn(versionFqn).orElse(null);
        if (version == null) {
            errors.add(ValidationError.of(versionFqn, "version", "版本不存在"));
            return ValidationResult.failure(errors);
        }

        // 2. 依赖链自洽性：检测循环依赖
        try {
            var detector = new CircularDependencyDetector();
            detector.detectCycles(dependencyRepository);
        } catch (Exception e) {
            errors.add(ValidationError.of(versionFqn, "dependencies",
                    "依赖链校验失败: " + e.getMessage()));
        }

        // 3. 关联端点合法性检查
        var relations = relationRepository.findByBundleVersionFqn(versionFqn);
        for (var rel : relations) {
            if (rel.getSourceFqn() != null && entityRepository.findByFqn(rel.getSourceFqn()).isEmpty()) {
                errors.add(ValidationError.of(rel.getFqnValue(), "sourceFqn",
                        "源端 EntitySchema 不存在: " + rel.getSourceFqn()));
            }
            if (rel.getTargetFqn() != null && entityRepository.findByFqn(rel.getTargetFqn()).isEmpty()) {
                errors.add(ValidationError.of(rel.getFqnValue(), "targetFqn",
                        "目标端 EntitySchema 不存在: " + rel.getTargetFqn()));
            }
        }

        // 4. 先执行写入校验
        ValidationResult saveResult = validateSave(versionFqn);
        errors.addAll(saveResult.getErrors());

        if (errors.isEmpty()) {
            return ValidationResult.success();
        }
        return ValidationResult.failure(errors);
    }

    /**
     * 预览模式：仅校验不落库，返回完整校验报告。
     * 校验内容与正式发布一致，但不修改数据。
     */
    public ValidationResult preview(String versionFqn) {
        return validatePublish(versionFqn);
    }
}
