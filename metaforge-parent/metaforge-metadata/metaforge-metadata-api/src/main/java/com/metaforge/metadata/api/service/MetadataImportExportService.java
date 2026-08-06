package com.metaforge.metadata.api.service;

import com.metaforge.metadata.api.annotation.OpenHostService;
import com.metaforge.metadata.api.dto.request.ExportRequest;
import com.metaforge.metadata.api.dto.request.ImportRequest;
import com.metaforge.metadata.api.dto.response.ExportResultDto;
import com.metaforge.metadata.api.dto.response.ImportResultDto;

/**
 * 元数据批量导入导出服务。
 * <p>
 * 导入支持 YAML/JSON 格式，以 FQN 为唯一标识，幂等支持"跳过/报错"两种策略。
 * 导入全程逐条执行结构校验，校验失败不影响其他合法数据。
 * 导入成功的数据仅写入草稿表，需手动执行生效后方可对外可见。
 * 导出支持按 FQN 前缀范围、元模型类型、指定 FQN 列表三种粒度。
 */
@OpenHostService
public interface MetadataImportExportService {

    /**
     * 批量导入元数据。
     * <p>
     * 逐条解析 → FQN 文法校验 → JSON Schema 结构校验 → 写入草稿表。
     * 单条失败不影响其他合法数据，返回完整导入结果清单。
     *
     * @param request 导入请求（含文件内容、格式、幂等策略）
     * @return 导入结果（成功/失败清单及失败原因）
     */
    ImportResultDto importMetadata(ImportRequest request);

    /**
     * 按 FQN 前缀范围导出生效元数据。
     * <p>
     * 导出指定 FQN 前缀匹配的所有生效元数据，结果按 FQN 排序。
     *
     * @param request 导出请求（含 fqnPrefixes 集合与导出格式）
     * @return 导出结果（含序列化内容与总数）
     */
    ExportResultDto exportByFqnPrefixes(ExportRequest request);

    /**
     * 按元模型类型导出生效元数据。
     * <p>
     * 导出属于指定 EntitySchema 的所有生效元数据。
     *
     * @param request 导出请求（含 entitySchemaFqn 与导出格式）
     * @return 导出结果
     */
    ExportResultDto exportByEntitySchema(ExportRequest request);

    /**
     * 按指定 FQN 列表导出生效元数据。
     * <p>
     * 精确导出指定 FQN 列表对应的生效元数据，不存在的 FQN 静默跳过。
     *
     * @param request 导出请求（含 fqns 列表与导出格式）
     * @return 导出结果
     */
    ExportResultDto exportByFqns(ExportRequest request);
}
