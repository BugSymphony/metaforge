package com.metaforge.metadata.api.service;

import com.metaforge.common.dto.PageRequest;
import com.metaforge.common.dto.PageResult;
import com.metaforge.metadata.api.annotation.OpenHostService;
import com.metaforge.metadata.api.dto.request.AdminQueryRequest;
import com.metaforge.metadata.api.dto.request.AttributeCondition;
import com.metaforge.metadata.api.dto.request.MetadataQueryRequest;
import com.metaforge.metadata.api.dto.response.MetadataEntityDto;

import java.util.List;

/**
 * 元数据查询检索服务。
 * <p>
 * 支持多种查询模式：FQN 精准查询、FQN 前缀范围查询（OR 并集逻辑）、
 * 元模型类型查询、属性条件组合查询、管理员全状态聚合查询。
 * 默认仅返回主表生效版本，支持分页与排序。
 */
@OpenHostService
public interface MetadataQueryService {

    /**
     * FQN 精准查询生效元数据完整内容。
     *
     * @param fqn 元数据 FQN
     * @return 元数据完整 DTO（含 content 全量字段）
     * @throws EntityNotFoundException 如果 FQN 不存在或已下线
     */
    MetadataEntityDto getByFqn(String fqn);

    /**
     * FQN 前缀范围查询生效元数据列表。
     * <p>
     * 支持传入多个前缀，按 OR 并集逻辑返回匹配任意前缀的所有生效元数据，
     * 结果按 FQN 排序，支持分页。
     *
     * @param request 查询请求（含 fqnPrefixes 集合与分页参数）
     * @return 分页结果
     */
    PageResult<MetadataEntityDto> listByFqnPrefixes(MetadataQueryRequest request);

    /**
     * 按元模型类型查询生效元数据列表。
     * <p>
     * 根据 entitySchemaFqn 过滤所有属于该元模型的生效元数据，支持分页。
     *
     * @param request 查询请求（含 entitySchemaFqn 与分页参数）
     * @return 分页结果
     */
    PageResult<MetadataEntityDto> listByEntitySchema(MetadataQueryRequest request);

    /**
     * 按属性条件组合查询生效元数据。
     * <p>
     * 支持精准匹配（字段值完全相等）与模糊匹配（字段值前缀匹配），
     * 多个条件之间为 AND 关系，支持分页。
     *
     * @param conditions  属性条件列表
     * @param pageRequest 分页请求
     * @return 分页结果
     */
    PageResult<MetadataEntityDto> queryByAttributes(List<AttributeCondition> conditions, PageRequest pageRequest);

    /**
     * 管理员专属全状态聚合查询。
     * <p>
     * 跨主表/草稿表/历史表聚合结果，每条数据标注状态（草稿/生效/历史归档）与来源表。
     * 仅管理员可调用。
     *
     * @param request 管理端查询请求（含状态过滤、FQN 过滤、分页参数）
     * @return 聚合查询分页结果
     */
    PageResult<MetadataEntityDto> adminQuery(AdminQueryRequest request);
}
