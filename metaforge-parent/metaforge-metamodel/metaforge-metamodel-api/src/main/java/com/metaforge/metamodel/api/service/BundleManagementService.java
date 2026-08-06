package com.metaforge.metamodel.api.service;

import com.metaforge.metamodel.api.dto.request.CreateBundleRequest;
import com.metaforge.metamodel.api.dto.response.BundleDto;

import java.util.List;
import java.util.Optional;

/**
 * Bundle 管理应用服务接口（契约层）。
 */
public interface BundleManagementService {

    /**
     * 创建 Bundle。
     */
    BundleDto create(CreateBundleRequest request);

    /**
     * 按 FQN 查询单个 Bundle。
     */
    Optional<BundleDto> findByFqn(String fqn);

    /**
     * 查询全部 Bundle 列表。
     */
    List<BundleDto> listAll();

    /**
     * 删除 Bundle（isSystem=true 的 Bundle 禁止删除）。
     */
    void delete(String fqn);
}
