package com.metaforge.metadata.api.service;

import com.metaforge.metadata.api.annotation.OpenHostService;
import com.metaforge.metadata.api.dto.request.CreateDraftRequest;
import com.metaforge.metadata.api.dto.request.UpdateDraftContentRequest;
import com.metaforge.metadata.api.dto.response.MetadataEntityDraftDto;

/**
 * 元数据草稿管理服务。
 * <p>
 * 草稿是元数据实体的编辑态副本，存储于 metadata_entity_draft 表，
 * 与主表（metadata_entity）物理隔离，对外完全不可见。
 * 每次保存时实时执行 JSON Schema 结构校验，仅校验通过的数据才允许写入。
 * 同一 FQN 最多仅允许存在一条草稿。
 */
@OpenHostService
public interface MetadataDraftService {

    /**
     * 基于已发布的 EntitySchema 创建全新元数据草稿。
     * <p>
     * 前置校验：
     * <ul>
     *   <li>FQN segment 符合 [A-Za-z][A-Za-z0-9_-]* 文法且不含保留分隔符 '.'</li>
     *   <li>FQN 全局唯一（主表 + 草稿表联合查重）</li>
     *   <li>entity_schema_fqn 对应版本已发布（非 DRAFT）</li>
     *   <li>若指定 parent_fqn，父实体必须已生效</li>
     *   <li>content 符合 EntitySchema JSON Schema 全字段结构校验</li>
     * </ul>
     *
     * @param request 草稿创建请求（含 fqn、name、entitySchemaFqn、content 等）
     * @return 创建成功的草稿 DTO
     * @throws FqnConflictException 如果 FQN 已存在于主表或草稿表
     * @throws MetadataValidationException 如果 JSON Schema 结构校验失败
     */
    MetadataEntityDraftDto createDraft(CreateDraftRequest request);

    /**
     * 基于已生效版本创建修改草稿。
     * <p>
     * 草稿内容从主表全量复制，base_version 记录原版本号。
     * 创建后允许自由编辑 content，但 fqn、parent_fqn、entity_schema_fqn 不可变更。
     *
     * @param fqn       已生效元数据的 FQN
     * @param createdBy 创建人标识
     * @return 基于生效版本创建的草稿 DTO
     * @throws EntityNotFoundException 如果 FQN 对应生效版本不存在
     * @throws FqnConflictException    如果该 FQN 已存在草稿
     */
    MetadataEntityDraftDto createDraftFromActive(String fqn, String createdBy);

    /**
     * 更新草稿的属性内容。
     * <p>
     * 仅允许修改 content 字段（新增/修改/删除属性字段）。
     * fqn、parent_fqn、entity_schema_fqn 创建后不可变更。
     * 更新时重新执行全字段 JSON Schema 结构校验。
     *
     * @param fqn     草稿 FQN
     * @param request 内容更新请求
     * @return 更新后的草稿 DTO
     * @throws DraftNotFoundException      如果草稿不存在
     * @throws MetadataValidationException 如果更新后的 content 校验失败
     */
    MetadataEntityDraftDto updateDraftContent(String fqn, UpdateDraftContentRequest request);

    /**
     * 查询草稿详情。
     *
     * @param fqn 草稿 FQN
     * @return 草稿 DTO
     * @throws DraftNotFoundException 如果草稿不存在
     */
    MetadataEntityDraftDto getDraft(String fqn);

    /**
     * 物理删除草稿。
     * <p>
     * 删除后草稿表无残留，主表与历史表无任何变更。
     * 该 FQN 可重新创建新草稿。
     *
     * @param fqn 草稿 FQN
     * @throws DraftNotFoundException 如果草稿不存在
     */
    void deleteDraft(String fqn);
}
