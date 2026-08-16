package com.metaforge.agent.cognition.api.port;

import com.metaforge.common.dto.PageRequest;
import com.metaforge.common.dto.PageResult;

import java.util.List;

/**
 * 元模型治理 BC 只读端口，生效态数据查询。
 * 上游 Provider: metamodel-governance (BundleManagementService, ElementDefinitionService, PackageManagementService, ExportManifestService, BundleDependencyService, BundleVersionManagementService)
 */
public interface MetamodelReadPort {

    Object getBundle(String fqn);

    PageResult<?> listBundles(PageRequest pageRequest);

    Object getEntitySchema(String fqn);

    PageResult<?> listEntitySchemas(Object query);

    Object getRelationSchema(String fqn);

    PageResult<?> listRelationSchemas(Object query);

    List<?> listPackages(String bundleVersionFqn);

    Object getExport(String bundleVersionFqn);

    boolean isPackageExported(String bundleVersionFqn, String packageFqn);

    Object getDependencyGraph(String bundleFqn);

    List<?> listBundleVersions(String bundleFqn);
}
