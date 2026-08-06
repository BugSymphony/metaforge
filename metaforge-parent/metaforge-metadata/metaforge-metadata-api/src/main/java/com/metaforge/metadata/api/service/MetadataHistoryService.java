package com.metaforge.metadata.api.service;

import com.metaforge.common.dto.PageRequest;
import com.metaforge.common.dto.PageResult;
import com.metaforge.metadata.api.annotation.OpenHostService;
import com.metaforge.metadata.api.dto.request.DiffRequest;
import com.metaforge.metadata.api.dto.response.EntityVersionDto;
import com.metaforge.metadata.api.dto.response.VersionDiffDto;

/**
 * 元数据历史版本追溯服务。
 * <p>
 * 历史表仅支持 INSERT 操作（数据库层面禁止 UPDATE 和 DELETE）。
 * 支持按 FQN 查询全历史版本列表、按 FQN+版本号查询单版本完整属性快照、
 * 以及任意两个历史版本间的字段级差异对比。
 */
@OpenHostService
public interface MetadataHistoryService {

    /**
     * 查询指定 FQN 的全历史版本列表。
     * <p>
     * 按版本号倒序排列，每条包含版本号、生效时间、操作人，
     * 默认不返回完整属性内容，支持分页。
     *
     * @param fqn         元数据 FQN
     * @param pageRequest 分页请求
     * @return 版本列表（倒序，分页）
     */
    PageResult<EntityVersionDto> listVersions(String fqn, PageRequest pageRequest);

    /**
     * 查询指定 FQN + 版本号的完整历史版本详情。
     * <p>
     * 返回该版本的完整属性快照、关联元模型 FQN、生效时间、操作人等全量信息。
     *
     * @param fqn     元数据 FQN
     * @param version 版本号
     * @return 历史版本完整 DTO
     * @throws VersionNotFoundException 如果指定版本不存在
     */
    EntityVersionDto getVersionDetail(String fqn, int version);

    /**
     * 对比任意两个历史版本的字段级差异。
     * <p>
     * 按"新增字段（ADDED）、修改字段（MODIFIED）、删除字段（DELETED）"三类分类展示变更内容。
     *
     * @param request 差异对比请求（含 fqn、versionA、versionB）
     * @return 差异对比结果
     * @throws VersionNotFoundException 如果任一版本不存在
     */
    VersionDiffDto compareVersions(DiffRequest request);
}
