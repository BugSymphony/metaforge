package com.metaforge.metamodel.application.service;

import com.metaforge.metamodel.api.dto.request.CreateBundleRequest;
import com.metaforge.metamodel.api.dto.response.BundleDto;
import com.metaforge.metamodel.api.service.BundleManagementService;
import com.metaforge.metamodel.domain.exception.FqnDuplicateException;
import com.metaforge.metamodel.domain.exception.FqnNotFoundException;
import com.metaforge.metamodel.domain.exception.PredefinedBundleProtectedException;
import com.metaforge.metamodel.domain.model.aggregate.Bundle;
import com.metaforge.metamodel.domain.model.aggregate.BundleVersion;
import com.metaforge.metamodel.domain.model.valueobject.BundleCode;
import com.metaforge.metamodel.domain.model.valueobject.SemanticVersion;
import com.metaforge.metamodel.domain.repository.BundleRepository;
import com.metaforge.metamodel.domain.repository.BundleVersionRepository;
import com.metaforge.metamodel.domain.service.FqnGenerator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Bundle 管理应用服务实现。
 */
@Service
@Transactional
public class BundleManagementServiceImpl implements BundleManagementService {

    private final BundleRepository bundleRepository;
    private final BundleVersionRepository versionRepository;
    private final FqnGenerator fqnGenerator;

    public BundleManagementServiceImpl(BundleRepository bundleRepository,
                                       BundleVersionRepository versionRepository,
                                       FqnGenerator fqnGenerator) {
        this.bundleRepository = bundleRepository;
        this.versionRepository = versionRepository;
        this.fqnGenerator = fqnGenerator;
    }

    @Override
    public BundleDto create(CreateBundleRequest request) {
        // 校验 FQN 唯一性
        if (bundleRepository.existsByFqn(request.getFqn())) {
            throw new FqnDuplicateException(request.getFqn());
        }

        BundleCode code = BundleCode.of(request.getFqn());
        Bundle bundle = Bundle.create(code, request.getName(),
                request.getDescription(), request.getOwner());

        Bundle saved = bundleRepository.save(bundle);

        // 创建初始草稿版本 v0.0.1（DRAFT）
        BundleVersion initialDraft = BundleVersion.createInitialDraft(
                fqnGenerator, code.getValue(), SemanticVersion.parse("0.0.1"));
        versionRepository.save(initialDraft);

        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BundleDto> findByFqn(String fqn) {
        return bundleRepository.findByFqn(fqn).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BundleDto> listAll() {
        return bundleRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private BundleDto toDto(Bundle bundle) {
        BundleDto dto = new BundleDto();
        dto.setFqn(bundle.getFqnValue());
        dto.setName(bundle.getName());
        dto.setDescription(bundle.getDescription());
        dto.setOwner(bundle.getOwner());
        dto.setSystem(bundle.isSystem());
        dto.setCreatedTime(bundle.getCreatedTime());
        dto.setUpdatedTime(bundle.getUpdatedTime());
        return dto;
    }

    @Override
    public void delete(String fqn) {
        Bundle bundle = bundleRepository.findByFqn(fqn)
                .orElseThrow(() -> new FqnNotFoundException(fqn));
        if (bundle.isSystem()) {
            throw new PredefinedBundleProtectedException(fqn, "DELETE");
        }
        bundleRepository.delete(bundle);
    }
}
