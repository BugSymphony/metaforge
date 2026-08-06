package com.metaforge.metadata.application.service;

import com.metaforge.common.dto.PageRequest;
import com.metaforge.common.dto.PageResult;
import com.metaforge.metadata.api.dto.request.DiffRequest;
import com.metaforge.metadata.api.dto.response.EntityVersionDto;
import com.metaforge.metadata.api.dto.response.FieldDiff;
import com.metaforge.metadata.api.dto.response.VersionDiffDto;
import com.metaforge.metadata.api.service.MetadataHistoryService;
import com.metaforge.metadata.domain.exception.EntityNotFoundException;
import com.metaforge.metadata.domain.model.entity.EntityVersion;
import com.metaforge.metadata.domain.repository.EntityVersionRepository;
import com.metaforge.metadata.domain.service.VersionDiffService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class MetadataHistoryServiceImpl implements MetadataHistoryService {

    private static final Logger log = LoggerFactory.getLogger(MetadataHistoryServiceImpl.class);

    private final EntityVersionRepository versionRepository;
    private final VersionDiffService versionDiffService;

    public MetadataHistoryServiceImpl(EntityVersionRepository versionRepository,
                                      VersionDiffService versionDiffService) {
        this.versionRepository = versionRepository;
        this.versionDiffService = versionDiffService;
    }

    @Override
    public PageResult<EntityVersionDto> listVersions(String fqn, PageRequest pageRequest) {
        List<EntityVersion> versions = versionRepository.findByFqnOrderByVersionDesc(fqn);

        int page = pageRequest != null ? pageRequest.getPage() : 1;
        int size = pageRequest != null ? pageRequest.getSize() : 20;

        int total = versions.size();
        int fromIndex = Math.min((page - 1) * size, total);
        int toIndex = Math.min(fromIndex + size, total);

        List<EntityVersionDto> dtos = versions.subList(fromIndex, toIndex).stream()
                .map(this::toVersionDto)
                .collect(Collectors.toList());

        return new PageResult<>(dtos, total, page, size);
    }

    @Override
    public EntityVersionDto getVersionDetail(String fqn, int version) {
        EntityVersion entityVersion = versionRepository.findByFqnAndVersion(fqn, version)
                .orElseThrow(() -> new EntityNotFoundException(
                        "历史版本不存在: fqn=" + fqn + ", version=" + version));
        return toVersionDto(entityVersion);
    }

    @Override
    public VersionDiffDto compareVersions(DiffRequest request) {
        log.info("版本差异对比: fqn={}, versionA={}, versionB={}",
                request.getFqn(), request.getVersionA(), request.getVersionB());

        EntityVersion versionA = versionRepository
                .findByFqnAndVersion(request.getFqn(), request.getVersionA())
                .orElseThrow(() -> new EntityNotFoundException(
                        "版本不存在: fqn=" + request.getFqn() + ", version=" + request.getVersionA()));

        EntityVersion versionB = versionRepository
                .findByFqnAndVersion(request.getFqn(), request.getVersionB())
                .orElseThrow(() -> new EntityNotFoundException(
                        "版本不存在: fqn=" + request.getFqn() + ", version=" + request.getVersionB()));

        List<FieldDiff> diffs = versionDiffService.compare(
                versionA.getContent(), versionB.getContent());

        VersionDiffDto result = new VersionDiffDto();
        result.setFqn(request.getFqn());
        result.setVersionA(request.getVersionA());
        result.setVersionB(request.getVersionB());
        result.setDiffs(diffs);
        result.setDiffTime(LocalDateTime.now());

        return result;
    }

    private EntityVersionDto toVersionDto(EntityVersion version) {
        EntityVersionDto dto = new EntityVersionDto();
        dto.setId(version.getId());
        dto.setFqn(version.getFqnValue());
        dto.setName(version.getName());
        dto.setDescription(version.getDescription());
        dto.setParentFqn(version.getParentFqn());
        dto.setVersion(version.getVersionValue());
        dto.setEntitySchemaFqn(version.getEntitySchemaFqnValue());
        dto.setContent(version.getContent());
        dto.setCreatedBy(version.getCreatedBy());
        dto.setCreatedTime(version.getCreatedTime());
        return dto;
    }
}
