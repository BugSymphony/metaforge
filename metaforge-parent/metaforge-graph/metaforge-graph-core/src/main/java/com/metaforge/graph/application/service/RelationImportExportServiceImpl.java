package com.metaforge.graph.application.service;

import com.metaforge.common.util.JsonbUtils;
import com.metaforge.graph.api.dto.*;
import com.metaforge.graph.api.service.RelationImportExportService;
import com.metaforge.graph.api.service.RelationDraftService;
import com.metaforge.graph.domain.model.aggregate.RelationInstance;
import com.metaforge.graph.domain.repository.RelationInstanceDraftRepository;
import com.metaforge.graph.domain.repository.RelationInstanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 关系实例批量导入导出服务实现。
 */
@Service
@Transactional
public class RelationImportExportServiceImpl implements RelationImportExportService {

    private static final Logger log = LoggerFactory.getLogger(RelationImportExportServiceImpl.class);

    private final RelationDraftService draftService;
    private final RelationInstanceRepository instanceRepository;
    private final RelationInstanceDraftRepository draftRepository;

    public RelationImportExportServiceImpl(RelationDraftService draftService,
                                            RelationInstanceRepository instanceRepository,
                                            RelationInstanceDraftRepository draftRepository) {
        this.draftService = draftService;
        this.instanceRepository = instanceRepository;
        this.draftRepository = draftRepository;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ImportResultDto importRelations(ImportRequest request) {
        log.info("批量导入: format={}, strategy={}", request.getFormat(), request.getStrategy());

        ImportResultDto result = new ImportResultDto();
        List<ImportItemResult> items = new ArrayList<>();

        if (request.getContent() == null || request.getContent().isBlank()) {
            result.setTotalCount(0);
            result.setItems(items);
            return result;
        }

        if (request.getContent().length() > 10 * 1024 * 1024) {
            result.setTotalCount(0);
            result.setFailureCount(1);
            items.add(ImportItemResult.failure("N/A", "content 大小超过 10MB 限制"));
            result.setItems(items);
            return result;
        }

        List<Map<String, Object>> records = parseImportContent(request.getContent(), request.getFormat());

        int success = 0;
        int skipped = 0;
        int failed = 0;

        for (Map<String, Object> record : records) {
            String fqn = (String) record.getOrDefault("fqn", "");
            try {
                fqn = importSingleRecord(record, request.getStrategy());
                items.add(ImportItemResult.success(fqn));
                success++;
            } catch (SkippedException e) {
                items.add(ImportItemResult.skip(e.getFqn()));
                skipped++;
            } catch (Exception e) {
                items.add(ImportItemResult.failure(fqn, e.getMessage()));
                failed++;
            }
        }

        result.setTotalCount(records.size());
        result.setSuccessCount(success);
        result.setSkipCount(skipped);
        result.setFailureCount(failed);
        result.setItems(items);

        log.info("导入完成: total={}, success={}, skip={}, fail={}", records.size(), success, skipped, failed);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ExportResultDto exportByFqnPrefixes(List<String> fqnPrefixes, String format) {
        log.info("按 FQN 前缀导出: prefixes={}", fqnPrefixes);
        List<Map<String, Object>> records = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String prefix : fqnPrefixes) {
            for (RelationInstance ri : instanceRepository.findByFqnPrefix(prefix)) {
                if (seen.add(ri.getFqnValue())) {
                    records.add(toExportRecord(ri));
                }
            }
        }
        return buildExportResult(records, format);
    }

    @Override
    @Transactional(readOnly = true)
    public ExportResultDto exportByRelationTypes(List<String> relationTypes, String format) {
        log.info("按关系类型导出: types={}", relationTypes);
        List<Map<String, Object>> records = new ArrayList<>();
        List<RelationInstance> all = instanceRepository.findBySourceEntityFqn("");
        for (RelationInstance ri : all) {
            if (relationTypes.contains(ri.getRelationType())) {
                records.add(toExportRecord(ri));
            }
        }
        return buildExportResult(records, format);
    }

    @Override
    @Transactional(readOnly = true)
    public ExportResultDto exportByFqns(List<String> fqns, String format) {
        log.info("按 FQN 列表导出: count={}", fqns != null ? fqns.size() : 0);
        List<Map<String, Object>> records = new ArrayList<>();
        if (fqns != null) {
            for (String fqn : fqns) {
                instanceRepository.findByFqnString(fqn).ifPresent(ri -> records.add(toExportRecord(ri)));
            }
        }
        return buildExportResult(records, format);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseImportContent(String content, String format) {
        try {
            if ("JSON".equalsIgnoreCase(format)) {
                return JsonbUtils.fromJsonb(content, List.class);
            }
            // YAML format - parse as JSON for MVP (YAML can be parsed by Jackson with snakeyaml)
            return JsonbUtils.fromJsonb(content, List.class);
        } catch (Exception e) {
            throw new RuntimeException("导入内容解析失败: " + e.getMessage(), e);
        }
    }

    private String importSingleRecord(Map<String, Object> record, String strategy) {
        String sourceEntityFqn = (String) record.get("sourceEntityFqn");
        String relationTypeFqn = (String) record.get("relationTypeFqn");
        String targetEntityFqn = (String) record.get("targetEntityFqn");
        String name = (String) record.getOrDefault("name", "导入关系");
        Map<String, Object> content = (Map<String, Object>) record.getOrDefault("content", new HashMap<>());

        String generatedFqn = sourceEntityFqn + "#" + relationTypeFqn + "#" + targetEntityFqn;

        if (instanceRepository.existsByFqnString(generatedFqn) || draftRepository.existsByFqn(generatedFqn)) {
            if ("ERROR".equalsIgnoreCase(strategy)) {
                throw new RuntimeException("FQN 已存在且策略为 ERROR: " + generatedFqn);
            }
            throw new SkippedException(generatedFqn, "已存在，跳过: " + generatedFqn);
        }

        CreateDraftRequest draftRequest = new CreateDraftRequest();
        draftRequest.setSourceEntityFqn(sourceEntityFqn);
        draftRequest.setRelationTypeFqn(relationTypeFqn);
        draftRequest.setTargetEntityFqn(targetEntityFqn);
        draftRequest.setName(name);
        draftRequest.setContent(content);

        draftService.createDraft(draftRequest);
        return generatedFqn;
    }

    private Map<String, Object> toExportRecord(RelationInstance ri) {
        Map<String, Object> record = new HashMap<>();
        record.put("fqn", ri.getFqnValue());
        record.put("name", ri.getNameValue());
        record.put("description", ri.getDescriptionValue());
        record.put("sourceEntityFqn", ri.getSourceEntityFqnValue());
        record.put("targetEntityFqn", ri.getTargetEntityFqnValue());
        record.put("relationType", ri.getRelationType());
        record.put("relationSchemaFqn", ri.getRelationSchemaFqnValue());
        record.put("content", ri.getContent());
        record.put("currentVersion", ri.getCurrentVersionValue());
        return record;
    }

    private ExportResultDto buildExportResult(List<Map<String, Object>> records, String format) {
        ExportResultDto result = new ExportResultDto();
        result.setTotalCount(records.size());
        result.setFormat(format);
        result.setContent(JsonbUtils.toJsonb(records));
        return result;
    }

    private static class SkippedException extends RuntimeException {
        private final String fqn;
        SkippedException(String fqn, String message) { super(message); this.fqn = fqn; }
        String getFqn() { return fqn; }
    }
}
