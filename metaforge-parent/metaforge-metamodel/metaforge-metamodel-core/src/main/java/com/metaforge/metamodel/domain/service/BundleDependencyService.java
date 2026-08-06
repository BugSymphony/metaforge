package com.metaforge.metamodel.domain.service;

import com.metaforge.metamodel.domain.exception.DependencyTargetNotFoundException;
import com.metaforge.metamodel.domain.exception.VersionNotDraftException;
import com.metaforge.metamodel.domain.model.aggregate.BundleVersion;
import com.metaforge.metamodel.domain.repository.BundleDependencyRepository;
import com.metaforge.metamodel.domain.repository.BundleVersionRepository;

import org.springframework.stereotype.Component;

/**
 * Bundle 依赖领域服务。
 * 声明跨 Bundle 精确版本依赖，校验目标存在性与循环依赖。
 */
@Component
public class BundleDependencyService {

    private final BundleVersionRepository versionRepository;
    private final BundleDependencyRepository dependencyRepository;
    private final CircularDependencyDetector circularDependencyDetector;

    public BundleDependencyService(BundleVersionRepository versionRepository,
                                    BundleDependencyRepository dependencyRepository,
                                    CircularDependencyDetector circularDependencyDetector) {
        this.versionRepository = versionRepository;
        this.dependencyRepository = dependencyRepository;
        this.circularDependencyDetector = circularDependencyDetector;
    }

    /**
     * 为当前草稿版本声明对目标已发布版本的依赖。
     *
     * @param sourceVersionFqn 当前草稿版本 FQN
     * @param targetVersionFqn 目标版本 FQN（精确版本）
     * @throws VersionNotDraftException         如果当前版本非草稿态
     * @throws DependencyTargetNotFoundException 如果目标版本不存在或未发布
     */
    public void declareDependency(String sourceVersionFqn, String targetVersionFqn) {
        BundleVersion sourceVersion = versionRepository.findByFqn(sourceVersionFqn)
                .orElseThrow(() -> new DependencyTargetNotFoundException(sourceVersionFqn));
        sourceVersion.requireDraft();

        BundleVersion targetVersion = versionRepository.findByFqn(targetVersionFqn)
                .orElseThrow(() -> new DependencyTargetNotFoundException(targetVersionFqn));

        if (!targetVersion.isPublished()) {
            throw new DependencyTargetNotFoundException(
                    targetVersionFqn + "（目标版本必须为已发布态）");
        }

        // 检测循环依赖
        circularDependencyDetector.validateNewDependency(
                dependencyRepository, sourceVersionFqn, targetVersionFqn);

        dependencyRepository.save(sourceVersionFqn, targetVersionFqn);
    }

    /**
     * 发布前校验当前版本的所有依赖。
     *
     * @param versionFqn 待发布版本 FQN
     */
    public void validateBeforePublish(String versionFqn) {
        BundleVersion version = versionRepository.findByFqn(versionFqn)
                .orElseThrow(() -> new DependencyTargetNotFoundException(versionFqn));

        // 检测全图循环依赖
        circularDependencyDetector.detectCycles(dependencyRepository);
    }
}
