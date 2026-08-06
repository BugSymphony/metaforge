package com.metaforge.graph.api.service;

import com.metaforge.graph.api.dto.CreateDraftRequest;
import com.metaforge.graph.api.dto.RelationInstanceDraftDto;
import com.metaforge.graph.api.dto.UpdateDraftContentRequest;

/**
 * 关系实例草稿管理服务。
 *
 * <p>草稿是关系实例的编辑态副本，存储于 relation_instance_draft 表，
 * 与主表 relation_instance 物理隔离，对外完全不可见。
 * 每次保存时实时执行 JSON Schema 结构校验，仅校验通过的数据才允许写入。
 * 同一 FQN 最多仅允许存在一条草稿。
 */
public interface RelationDraftService {

    /**
     * 基于已发布的 RelationSchema 手动创建全新的关系草稿。
     *
     * @param request 草稿创建请求
     * @return 创建成功的草稿 DTO
     */
    RelationInstanceDraftDto createDraft(CreateDraftRequest request);

    /**
     * 基于已生效版本创建修改草稿。
     *
     * @param fqn 已生效关系实例的 FQN
     * @return 基于生效版本创建的草稿 DTO
     */
    RelationInstanceDraftDto createDraftFromActive(String fqn);

    /**
     * 更新草稿的属性内容。
     *
     * @param fqn 草稿 FQN
     * @param request 内容更新请求
     * @return 更新后的草稿 DTO
     */
    RelationInstanceDraftDto updateDraftContent(String fqn, UpdateDraftContentRequest request);

    /**
     * 查询草稿详情。
     *
     * @param fqn 草稿 FQN
     * @return 草稿 DTO
     */
    RelationInstanceDraftDto getDraft(String fqn);

    /**
     * 物理删除草稿。
     *
     * @param fqn 草稿 FQN
     */
    void deleteDraft(String fqn);
}
