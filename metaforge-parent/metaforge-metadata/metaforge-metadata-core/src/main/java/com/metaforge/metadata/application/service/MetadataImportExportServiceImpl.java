package com.metaforge.metadata.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.metaforge.common.util.JsonbUtils;
import com.metaforge.metadata.api.dto.request.ExportRequest;
import com.metaforge.metadata.api.dto.request.ImportRequest;
import com.metaforge.metadata.api.dto.response.ExportResultDto;
import com.metaforge.metadata.api.dto.response.ImportItemResult;
import com.metaforge.metadata.api.dto.response.ImportResultDto;
import com.metaforge.metadata.api.dto.response.ValidationErrorDetailDto;
import com.metaforge.metadata.api.enums.ExportFormat;
import com.metaforge.metadata.api.enums.ImportFormat;
import com.metaforge.metadata.api.enums.ImportStrategy;
import com.metaforge.metadata.api.service.MetadataImportExportService;
import com.metaforge.metadata.domain.model.aggregate.MetadataEntity;
import com.metaforge.metadata.domain.model.aggregate.MetadataEntityDraft;
import com.metaforge.metadata.domain.model.valueobject.EntitySchemaFQN;
import com.metaforge.metadata.domain.model.valueobject.FQN;
import com.metaforge.metadata.domain.repository.MetadataEntityDraftRepository;
import com.metaforge.metadata.domain.repository.MetadataEntityRepository;
import com.metaforge.metadata.domain.service.SchemaValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class MetadataImportExportServiceImpl implements MetadataImportExportService {

    private static final Logger log = LoggerFactory.getLogger(MetadataImportExportServiceImpl.class);

    private final ObjectMapper objectMapper;
    private final MetadataEntityRepository entityRepository;
    private final MetadataEntityDraftRepository draftRepository;
    private final SchemaValidationService schemaValidationService;

    public MetadataImportExportServiceImpl(ObjectMapper objectMapper,
                                           MetadataEntityRepository entityRepository,
                                           MetadataEntityDraftRepository draftRepository,
                                           SchemaValidationService schemaValidationService) {
        this.objectMapper = objectMapper;
        this.entityRepository = entityRepository;
        this.draftRepository = draftRepository;
        this.schemaValidationService = schemaValidationService;
    }

    @Override
    public ImportResultDto importMetadata(ImportRequest request) {
        log.info("批量导入元数据: format={}, strategy={}", request.getFormat(), request.getStrategy());

        List<ImportItem> importItems = parseImportContent(request.getContent(), request.getFormat());
        String createdBy = request.getCreatedBy() != null ? request.getCreatedBy() : "system";

        List<ImportItemResult> results = new ArrayList<>();
        int successCount = 0;
        int skipCount = 0;
        int errorCount = 0;

        for (ImportItem item : importItems) {
            try {
                ImportItemResult result = processImportItem(item, request.getStrategy(), createdBy);
                results.add(result);
                if (result.isSuccess()) {
                    successCount++;
                } else {
                    if (request.getStrategy() == ImportStrategy.SKIP) {
                        skipCount++;
                    } else {
                        errorCount++;
                    }
                }
            } catch (Exception e) {
                ImportItemResult result = new ImportItemResult();
                result.setFqn(item.fqn);
                result.setSuccess(false);
                result.setMessage("导入异常: " + e.getMessage());
                results.add(result);
                errorCount++;
                log.error("导入条目失败: fqn={}", item.fqn, e);
            }
        }

        ImportResultDto resultDto = new ImportResultDto();
        resultDto.setTotalCount(importItems.size());
        resultDto.setSuccessCount(successCount);
        resultDto.setSkipCount(skipCount);
        resultDto.setErrorCount(errorCount);
        resultDto.setItems(results);

        log.info("导入完成: total={}, success={}, skip={}, error={}",
                importItems.size(), successCount, skipCount, errorCount);
        return resultDto;
    }

    @Override
    public ExportResultDto exportByFqnPrefixes(ExportRequest request) {
        List<MetadataEntity> entities = entityRepository.findByFqnPrefixIn(request.getFqnPrefixes());
        return buildExportResult(entities, request.getFormat());
    }

    @Override
    public ExportResultDto exportByEntitySchema(ExportRequest request) {
        List<MetadataEntity> entities = entityRepository.findByEntitySchemaFqn(request.getEntitySchemaFqn());
        return buildExportResult(entities, request.getFormat());
    }

    @Override
    public ExportResultDto exportByFqns(ExportRequest request) {
        List<MetadataEntity> entities = new ArrayList<>();
        if (request.getFqns() != null) {
            for (String fqn : request.getFqns()) {
                entityRepository.findByFqn(fqn).ifPresent(entities::add);
            }
        }
        return buildExportResult(entities, request.getFormat());
    }

    /**
     * 批量合规校验。
     * 按元模型类型或 FQN 前缀范围遍历主表生效元数据逐条执行 JSON Schema 校验，
     * 输出包含通过率、违规 FQN 清单、违规详情的校验报告。
     */
    public ImportResultDto validateBatch(String entitySchemaFqn, List<String> fqnPrefixes) {
        List<MetadataEntity> entities;
        if (entitySchemaFqn != null && !entitySchemaFqn.isEmpty()) {
            entities = entityRepository.findByEntitySchemaFqn(entitySchemaFqn);
        } else if (fqnPrefixes != null && !fqnPrefixes.isEmpty()) {
            entities = entityRepository.findByFqnPrefixIn(fqnPrefixes);
        } else {
            ImportResultDto empty = new ImportResultDto();
            empty.setTotalCount(0);
            empty.setSuccessCount(0);
            empty.setErrorCount(0);
            empty.setItems(Collections.emptyList());
            return empty;
        }

        List<ImportItemResult> results = new ArrayList<>();
        int totalCount = entities.size();
        int passCount = 0;
        int failCount = 0;

        for (MetadataEntity entity : entities) {
            try {
                List<ValidationErrorDetailDto> errors = schemaValidationService
                        .validateAndReturnErrors(entity.getEntitySchemaFqnValue(), entity.getContent());

                ImportItemResult result = new ImportItemResult();
                result.setFqn(entity.getFqnValue());
                if (errors.isEmpty()) {
                    result.setSuccess(true);
                    result.setMessage("校验通过");
                    passCount++;
                } else {
                    result.setSuccess(false);
                    result.setMessage("违规项: " + errors.stream()
                            .map(e -> e.getJsonPath() + " " + e.getMessage())
                            .collect(Collectors.joining("; ")));
                    failCount++;
                }
                results.add(result);
            } catch (Exception e) {
                ImportItemResult result = new ImportItemResult();
                result.setFqn(entity.getFqnValue());
                result.setSuccess(false);
                result.setMessage("校验异常: " + e.getMessage());
                results.add(result);
                failCount++;
                log.error("批量校验异常: fqn={}", entity.getFqnValue(), e);
            }
        }

        ImportResultDto resultDto = new ImportResultDto();
        resultDto.setTotalCount(totalCount);
        resultDto.setSuccessCount(passCount);
        resultDto.setSkipCount(0);
        resultDto.setErrorCount(failCount);
        resultDto.setItems(results);

        log.info("批量校验完成: total={}, pass={}, fail={}", totalCount, passCount, failCount);
        return resultDto;
    }

    private ImportItemResult processImportItem(ImportItem item, ImportStrategy strategy, String createdBy) {
        ImportItemResult result = new ImportItemResult();
        result.setFqn(item.fqn);

        if (draftRepository.existsByFqn(item.fqn) || entityRepository.existsByFqn(item.fqn)) {
            if (strategy == ImportStrategy.SKIP) {
                result.setSuccess(false);
                result.setMessage("FQN 已存在，跳过: " + item.fqn);
                return result;
            } else {
                result.setSuccess(false);
                result.setMessage("FQN 冲突: " + item.fqn);
                return result;
            }
        }

        try {
            if (item.entitySchemaFqn != null && item.content != null) {
                schemaValidationService.validate(item.entitySchemaFqn, item.content);
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("JSON Schema 校验失败: " + e.getMessage());
            return result;
        }

        MetadataEntityDraft draft = new MetadataEntityDraft(
                FQN.of(item.fqn),
                item.name != null ? item.name : item.fqn,
                item.description,
                item.parentFqn,
                item.entitySchemaFqn != null ? EntitySchemaFQN.of(item.entitySchemaFqn) : null,
                item.content != null ? item.content : Collections.emptyMap(),
                null,
                createdBy);

        draftRepository.save(draft);
        result.setSuccess(true);
        result.setMessage("导入成功，已写入草稿表");
        return result;
    }

    private List<ImportItem> parseImportContent(String content, ImportFormat format) {
        if (content == null || content.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(content, new TypeReference<List<ImportItem>>() {});
        } catch (Exception e) {
            log.error("导入内容解析失败: format={}", format, e);
            throw new RuntimeException("导入内容解析失败: " + e.getMessage(), e);
        }
    }

    private ExportResultDto buildExportResult(List<MetadataEntity> entities, ExportFormat format) {
        List<Map<String, Object>> exportData = entities.stream()
                .map(e -> {
                    Map<String, Object> map = new java.util.LinkedHashMap<>();
                    map.put("fqn", e.getFqnValue());
                    map.put("name", e.getName());
                    map.put("description", e.getDescription());
                    map.put("parentFqn", e.getParentFqn());
                    map.put("entitySchemaFqn", e.getEntitySchemaFqnValue());
                    map.put("content", e.getContent());
                    map.put("currentVersion", e.getCurrentVersionValue());
                    return map;
                })
                .collect(Collectors.toList());

        String serialized;
        ExportFormat resultFormat = format != null ? format : ExportFormat.JSON;
        try {
            serialized = JsonbUtils.toJsonb(exportData);
        } catch (Exception e) {
            log.error("导出序列化失败", e);
            serialized = "[]";
        }

        ExportResultDto result = new ExportResultDto();
        result.setFormat(resultFormat);
        result.setContent(serialized);
        result.setEntityCount(entities.size());
        return result;
    }

    @SuppressWarnings("unused")
    private static class ImportItem {
        public String fqn;
        public String name;
        public String description;
        public String parentFqn;
        public String entitySchemaFqn;
        public Map<String, Object> content;
    }
}
