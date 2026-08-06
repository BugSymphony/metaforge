package com.metaforge.graph.api.service;

import com.metaforge.graph.api.dto.DiffRequest;
import com.metaforge.graph.api.dto.RelationVersionDto;
import com.metaforge.graph.api.dto.VersionDiffDto;

import java.util.List;

/**
 * 关系实例历史版本追溯服务。
 *
 * <p>历史表仅支持 INSERT 操作（数据库层面禁止 UPDATE 和 DELETE）。
 * 支持按 FQN 查询全历史版本列表、按 FQN+版本号查询单版本完整属性快照、
 * 以及任意两个历史版本间的字段级差异对比。
 */
public interface RelationHistoryService {

    List<RelationVersionDto> listVersions(String fqn);

    RelationVersionDto getVersionDetail(String fqn, int version);

    VersionDiffDto compareVersions(DiffRequest request);
}
