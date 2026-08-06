package com.metaforge.graph.application.service;

import com.metaforge.common.util.JsonbUtils;
import com.metaforge.graph.api.dto.DiffRequest;
import com.metaforge.graph.api.dto.FieldDiff;
import com.metaforge.graph.api.dto.RelationVersionDto;
import com.metaforge.graph.api.dto.VersionDiffDto;
import com.metaforge.graph.api.service.RelationHistoryService;
import com.metaforge.graph.infrastructure.config.GraphBizException;
import com.metaforge.graph.domain.model.aggregate.RelationVersion;
import com.metaforge.graph.domain.repository.RelationVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.metaforge.graph.api.constant.GraphErrorCode.VERSION_NOT_FOUND;

/**
 * 关系实例历史版本追溯服务实现。
 * 支持版本列表、详情查询与两版本字段级差异对比。
 */
@Service
@Transactional(readOnly = true)
public class RelationHistoryServiceImpl implements RelationHistoryService {

    private static final Logger log = LoggerFactory.getLogger(RelationHistoryServiceImpl.class);

    private final RelationVersionRepository versionRepository;

    public RelationHistoryServiceImpl(RelationVersionRepository versionRepository) {
        this.versionRepository = versionRepository;
    }

    @Override
    public List<RelationVersionDto> listVersions(String fqn) {
        log.debug("查询历史版本列表: fqn={}", fqn);
        List<RelationVersion> versions = versionRepository.findByFqnOrderByVersionDesc(fqn);
        return versions.stream()
                .map(this::toVersionDto)
                .toList();
    }

    @Override
    public RelationVersionDto getVersionDetail(String fqn, int version) {
        log.debug("查询版本详情: fqn={}, version={}", fqn, version);
        RelationVersion rv = versionRepository.findByFqnAndVersion(fqn, version)
                .orElseThrow(() -> new VersionNotFoundException("版本不存在: " + fqn + " v" + version));
        return toVersionDtoFull(rv);
    }

    @Override
    public VersionDiffDto compareVersions(DiffRequest request) {
        log.info("版本差异对比: fqn={}, v{} vs v{}", request.getFqn(), request.getVersionA(), request.getVersionB());

        RelationVersion vA = versionRepository.findByFqnAndVersion(request.getFqn(), request.getVersionA())
                .orElseThrow(() -> new VersionNotFoundException(
                        "版本不存在: " + request.getFqn() + " v" + request.getVersionA()));

        RelationVersion vB = versionRepository.findByFqnAndVersion(request.getFqn(), request.getVersionB())
                .orElseThrow(() -> new VersionNotFoundException(
                        "版本不存在: " + request.getFqn() + " v" + request.getVersionB()));

        VersionDiffDto result = new VersionDiffDto();
        result.setFqn(request.getFqn());
        result.setVersionA(request.getVersionA());
        result.setVersionB(request.getVersionB());

        Map<String, Object> contentA = parseContent(vA.getContent());
        Map<String, Object> contentB = parseContent(vB.getContent());

        List<FieldDiff> added = new ArrayList<>();
        List<FieldDiff> modified = new ArrayList<>();
        List<FieldDiff> deleted = new ArrayList<>();

        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(contentA.keySet());
        allKeys.addAll(contentB.keySet());

        for (String key : allKeys) {
            boolean inA = contentA.containsKey(key);
            boolean inB = contentB.containsKey(key);
            Object valA = contentA.get(key);
            Object valB = contentB.get(key);

            if (!inA && inB) {
                added.add(FieldDiff.added(key, valB));
            } else if (inA && !inB) {
                deleted.add(FieldDiff.deleted(key, valA));
            } else if (!Objects.equals(valA, valB)) {
                modified.add(FieldDiff.modified(key, valA, valB));
            }
        }

        result.setAddedFields(added);
        result.setModifiedFields(modified);
        result.setDeletedFields(deleted);

        log.info("差异对比完成: added={}, modified={}, deleted={}", added.size(), modified.size(), deleted.size());
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseContent(String jsonb) {
        if (jsonb == null || jsonb.isEmpty()) {
            return Collections.emptyMap();
        }
        return JsonbUtils.fromJsonb(jsonb, Map.class);
    }

    private RelationVersionDto toVersionDto(RelationVersion v) {
        RelationVersionDto dto = new RelationVersionDto();
        dto.setId(v.getId());
        dto.setFqn(v.getFqn());
        dto.setName(v.getName());
        dto.setDescription(v.getDescription());
        dto.setSourceEntityFqn(v.getSourceEntityFqn());
        dto.setTargetEntityFqn(v.getTargetEntityFqn());
        dto.setRelationType(v.getRelationType());
        dto.setRelationSchemaFqn(v.getRelationSchemaFqn());
        dto.setVersion(v.getVersion());
        dto.setActivatedBy(v.getActivatedBy());
        dto.setActivatedTime(v.getActivatedTime());
        return dto;
    }

    private RelationVersionDto toVersionDtoFull(RelationVersion v) {
        RelationVersionDto dto = toVersionDto(v);
        dto.setContent(parseContent(v.getContent()));
        return dto;
    }

    public static class VersionNotFoundException extends GraphBizException {
        public VersionNotFoundException(String message) {
            super(VERSION_NOT_FOUND, message);
        }
        @Override
        public String getErrorCodeName() { return "VERSION_NOT_FOUND"; }
    }
}
