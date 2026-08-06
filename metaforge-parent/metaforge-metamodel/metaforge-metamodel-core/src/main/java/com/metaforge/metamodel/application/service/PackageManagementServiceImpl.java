package com.metaforge.metamodel.application.service;

import com.metaforge.metamodel.api.dto.request.CreatePackageRequest;
import com.metaforge.metamodel.api.dto.response.PackageDto;
import com.metaforge.metamodel.api.service.PackageManagementService;
import com.metaforge.metamodel.domain.exception.FqnDuplicateException;
import com.metaforge.metamodel.domain.exception.FqnNotFoundException;
import com.metaforge.metamodel.domain.exception.PackageDepthExceededException;
import com.metaforge.metamodel.domain.model.entity.Package;
import com.metaforge.metamodel.domain.model.valueobject.Fqn;
import com.metaforge.metamodel.domain.repository.PackageRepository;
import com.metaforge.metamodel.domain.service.FqnGenerator;
import com.metaforge.metamodel.infrastructure.config.MetamodelProperties;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Package 管理应用服务实现。
 */
@Service
@Transactional
public class PackageManagementServiceImpl implements PackageManagementService {

    private final PackageRepository packageRepository;
    private final FqnGenerator fqnGenerator;
    private final MetamodelProperties properties;

    public PackageManagementServiceImpl(PackageRepository packageRepository,
                                         FqnGenerator fqnGenerator,
                                         MetamodelProperties properties) {
        this.packageRepository = packageRepository;
        this.fqnGenerator = fqnGenerator;
        this.properties = properties;
    }

    @Override
    public PackageDto create(CreatePackageRequest request) {
        int parentDepth = -1;
        String parentFqn = request.getParentPackageFqn();

        // 根层 Package：FQN = {bundleVersionFqn}.{segment}
        // 子层 Package：FQN = {parentFqn}.{segment}
        String fqnStr;
        if (parentFqn == null || parentFqn.isBlank()) {
            fqnStr = fqnGenerator.package_(request.getBundleVersionFqn(), request.getSegment());
        } else {
            // 查找父 Package 获取深度
            Package parentPkg = packageRepository.findByFqn(parentFqn)
                    .orElseThrow(() -> new FqnNotFoundException(parentFqn));
            parentDepth = parentPkg.getDepth();
            fqnStr = fqnGenerator.package_(parentFqn, request.getSegment());
        }

        // 校验 FQN 唯一性
        if (packageRepository.findByFqn(fqnStr).isPresent()) {
            throw new FqnDuplicateException(fqnStr);
        }

        Package pkg = Package.create(Fqn.of(fqnStr), request.getBundleVersionFqn(),
                parentFqn, request.getDescription(), parentDepth,
                properties.getMaxPackageDepth());

        Package saved = packageRepository.save(pkg);
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PackageDto> findByFqn(String fqn) {
        return packageRepository.findByFqn(fqn).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PackageDto> listByBundleVersion(String bundleVersionFqn) {
        return packageRepository.findByBundleVersionFqn(bundleVersionFqn).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(String fqn) {
        Package pkg = packageRepository.findByFqn(fqn)
                .orElseThrow(() -> new FqnNotFoundException(fqn));

        // 校验 Package 下是否存在子元素
        if (packageRepository.existsByFqnPrefix(fqn + ".")) {
            throw new IllegalStateException("Package " + fqn + " 下存在子元素或子 Package，不可删除");
        }

        packageRepository.delete(pkg);
    }

    private PackageDto toDto(Package pkg) {
        PackageDto dto = new PackageDto();
        dto.setFqn(pkg.getFqnValue());
        dto.setBundleVersionFqn(pkg.getBundleVersionFqn());
        dto.setParentPackageFqn(pkg.getParentPackageFqn());
        dto.setDescription(pkg.getDescription());
        dto.setDepth(pkg.getDepth());
        dto.setCreatedTime(pkg.getCreatedTime());
        dto.setUpdatedTime(pkg.getUpdatedTime());
        return dto;
    }
}
