package com.metaforge.agent.cognition.core.domain.port;

import java.util.List;

public interface MetadataClientPort {

    Object getByFqn(String fqn);

    Object listByFqnPrefixes(List<String> fqnPrefixes, int page, int size);

    Object listByEntitySchema(String entitySchemaFqn, int page, int size);

    Object queryByAttributes(List<Object> conditions, String matchMode);
}
