package com.metaforge.metamodel.domain.service;

import com.metaforge.metamodel.domain.exception.ExportValidationException;
import com.metaforge.metamodel.domain.repository.PackageRepository;
import com.metaforge.metamodel.domain.repository.RelationSchemaRepository;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 导出校验领域服务。
 * 验证导出清单中包含的 RelationSchema 端点可达性，
 * 以及导出 Package 是否存在于当前 BundleVersion 中。
 */
@Component
public class ExportValidationService {

    private final PackageRepository packageRepository;
    private final RelationSchemaRepository relationRepository;

    public ExportValidationService(PackageRepository packageRepository,
                                    RelationSchemaRepository relationRepository) {
        this.packageRepository = packageRepository;
        this.relationRepository = relationRepository;
    }

    /**
     * 校验导出 Package FQN 列表。
     * 1. 所有导出的 Package 必须存在于当前 BundleVersion 中
     * 2. 导出包含 RelationSchema 的 Package 时，关系两端 EntitySchema 所在包必须均已导出
     *
     * @param bundleVersionFqn    所属 BundleVersion FQN
     * @param exportedPackageFqns 待导出的 Package FQN 列表
     * @throws ExportValidationException 校验失败
     */
    public void validateExport(String bundleVersionFqn, List<String> exportedPackageFqns) {
        if (exportedPackageFqns == null || exportedPackageFqns.isEmpty()) {
            return;
        }

        // 校验 1: Package 存在性
        for (String pkgFqn : exportedPackageFqns) {
            if (packageRepository.findByFqn(pkgFqn).isEmpty()) {
                throw new ExportValidationException(
                        "导出清单包含不存在的 Package: " + pkgFqn);
            }
        }

        // 校验 2: RelationSchema 端点可达性
        for (String pkgFqn : exportedPackageFqns) {
            var relations = relationRepository.findByPackageFqn(pkgFqn);
            for (var rel : relations) {
                String sourcePkg = extractPackageFqn(rel.getSourceFqn());
                String targetPkg = extractPackageFqn(rel.getTargetFqn());

                if (!exportedPackageFqns.contains(sourcePkg)) {
                    throw new ExportValidationException(
                            "RelationSchema " + rel.getFqnValue()
                                    + " 的源端 EntitySchema 所在 Package " + sourcePkg + " 未导出");
                }
                if (!exportedPackageFqns.contains(targetPkg)) {
                    throw new ExportValidationException(
                            "RelationSchema " + rel.getFqnValue()
                                    + " 的目标端 EntitySchema 所在 Package " + targetPkg + " 未导出");
                }
            }
        }
    }

    /**
     * 从 EntitySchema FQN 中提取所属 Package FQN。
     * 例如 order:1.0.0.pkg_order.Order → order:1.0.0.pkg_order
     */
    private String extractPackageFqn(String entityFqn) {
        if (entityFqn == null) return null;
        int lastDot = entityFqn.lastIndexOf('.');
        return lastDot > 0 ? entityFqn.substring(0, lastDot) : entityFqn;
    }
}
