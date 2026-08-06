package com.metaforge.graph.api.service;

import com.metaforge.graph.api.dto.ExportResultDto;
import com.metaforge.graph.api.dto.ImportRequest;
import com.metaforge.graph.api.dto.ImportResultDto;

import java.util.List;

/**
 * 关系实例批量导入导出服务。
 *
 * <p>导入支持 YAML/JSON 格式，导入默认进入草稿态，禁止直接写入主表。
 * 导出支持按 FQN 前缀、关系类型、指定 FQN 列表三种粒度。
 */
public interface RelationImportExportService {

    ImportResultDto importRelations(ImportRequest request);

    ExportResultDto exportByFqnPrefixes(List<String> fqnPrefixes, String format);

    ExportResultDto exportByRelationTypes(List<String> relationTypes, String format);

    ExportResultDto exportByFqns(List<String> fqns, String format);
}
