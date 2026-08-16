package com.metaforge.agent.cognition.api.port;

import com.metaforge.common.dto.PageRequest;
import com.metaforge.common.dto.PageResult;

import java.util.List;

/**
 * 语义关系网络 BC 只读端口，生效态数据查询。
 * 上游 Provider: semantic-relation-network (RelationQueryService, RelationTopologyService)
 */
public interface GraphReadPort {

    Object getByFqn(String fqn);

    List<?> getOutboundRelations(String entityFqn, String relationType, String targetEntityType);

    List<?> getInboundRelations(String entityFqn, String relationType, String sourceEntityType);

    PageResult<?> multiFilter(Object request);

    Object getRelationCount(String entityFqn);

    PageResult<?> listByConditions(String fqnPrefix, String relationSchemaFqn, PageRequest pageRequest);
}
