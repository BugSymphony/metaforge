package com.metaforge.graph.api.service;

import com.metaforge.common.dto.PageResult;
import com.metaforge.graph.api.dto.AdminQueryRequest;
import com.metaforge.graph.api.dto.RelationInstanceDto;
import com.metaforge.graph.api.dto.RelationQueryRequest;

import java.util.List;

/**
 * 关系实例查询检索服务。
 *
 * <p>支持五种查询模式：FQN 精准查询、指定实体出入边查询、
 * FQN 前缀范围查询、多维过滤查询、管理员全状态聚合查询。
 * 默认仅返回主表生效版本，支持分页与排序。
 */
public interface RelationQueryService {

    RelationInstanceDto getByFqn(String fqn);

    List<RelationInstanceDto> getOutboundRelations(String entityFqn, String relationType, String targetEntityType);

    List<RelationInstanceDto> getInboundRelations(String entityFqn, String relationType, String sourceEntityType);

    PageResult<RelationInstanceDto> listByConditions(String fqnPrefix, String relationSchemaFqn,
                                                      com.metaforge.common.dto.PageRequest pageRequest);

    PageResult<RelationInstanceDto> multiFilter(RelationQueryRequest request);

    PageResult<RelationInstanceDto> adminQuery(AdminQueryRequest request);
}
