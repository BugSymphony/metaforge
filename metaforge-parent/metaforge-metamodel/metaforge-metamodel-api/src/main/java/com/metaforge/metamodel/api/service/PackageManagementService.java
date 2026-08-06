package com.metaforge.metamodel.api.service;

import com.metaforge.metamodel.api.dto.request.CreatePackageRequest;
import com.metaforge.metamodel.api.dto.response.PackageDto;

import java.util.List;
import java.util.Optional;

/**
 * Package 管理应用服务接口（契约层）。
 */
public interface PackageManagementService {

    /**
     * 在指定 BundleVersion 下创建 Package。
     */
    PackageDto create(CreatePackageRequest request);

    /**
     * 按 FQN 查询单个 Package。
     */
    Optional<PackageDto> findByFqn(String fqn);

    /**
     * 查询指定 BundleVersion 的所有 Package。
     */
    List<PackageDto> listByBundleVersion(String bundleVersionFqn);

    /**
     * 删除指定 Package（需为空）。
     */
    void delete(String fqn);
}
