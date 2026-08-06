package com.metaforge.metamodel.api.service;

import com.metaforge.metamodel.api.dto.request.CreateDraftRequest;
import com.metaforge.metamodel.api.dto.response.BundleVersionDto;

import java.util.List;
import java.util.Optional;

/**
 * BundleVersion 管理应用服务接口（契约层）。
 */
public interface BundleVersionManagementService {

    /**
     * 从最新已发布版本创建草稿。
     */
    BundleVersionDto createDraft(CreateDraftRequest request);

    /**
     * 发布指定版本的草稿。
     */
    BundleVersionDto publish(String versionFqn);

    /**
     * 按 FQN 查询单个版本。
     */
    Optional<BundleVersionDto> findByFqn(String fqn);

    /**
     * 查询指定 Bundle 的所有版本（按创建时间倒序）。
     */
    List<BundleVersionDto> listByBundle(String bundleFqn);

    /**
     * 声明跨 Bundle 依赖（精确版本）。
     * 当前版本必须是草稿态，目标版本必须已发布。
     */
    void declareDependency(String sourceVersionFqn, String targetVersionFqn);
}
