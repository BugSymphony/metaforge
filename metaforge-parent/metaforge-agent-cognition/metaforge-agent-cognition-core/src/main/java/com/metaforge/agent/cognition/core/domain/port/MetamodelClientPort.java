package com.metaforge.agent.cognition.core.domain.port;

import java.util.List;

public interface MetamodelClientPort {

    Object getBundle(String fqn);

    Object listBundles(int page, int size);

    String getLatestPublishedVersion(String bundleFqn);

    Object getEntitySchema(String fqn);

    Object listEntitySchemasByPrefixes(List<String> fqnPrefixes);

    String resolveBundleFqnByPrefix(String fqnPrefix);
}
