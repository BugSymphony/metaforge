package com.metaforge.agent.cognition.api.port;

import com.metaforge.common.dto.PageRequest;
import com.metaforge.common.dto.PageResult;

import java.util.List;

/**
 * 元数据管理 BC 只读端口，生效态数据查询。
 * 上游 Provider: metadata-management (MetadataQueryService)
 */
public interface MetadataReadPort {

    Object getByFqn(String fqn);

    PageResult<?> listByFqnPrefixes(List<String> fqnPrefixes, PageRequest pageRequest);

    PageResult<?> listByEntitySchema(String entitySchemaFqn, PageRequest pageRequest);
}
