package com.metaforge.agent.cognition.core.infrastructure.adapter;

import com.metaforge.agent.cognition.api.port.MetamodelReadPort;
import com.metaforge.metamodel.api.dto.ElementQueryRequest;
import com.metaforge.metamodel.api.service.*;
import com.metaforge.common.dto.PageRequest;
import com.metaforge.common.dto.PageResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class MetamodelReadPortAdapter implements MetamodelReadPort {

    private final BundleManagementService bundleManagementService;
    private final BundleVersionManagementService bundleVersionManagementService;
    private final ElementDefinitionService elementDefinitionService;
    private final PackageManagementService packageManagementService;
    private final ExportManifestService exportManifestService;

    public MetamodelReadPortAdapter(BundleManagementService bundleManagementService,
                                     BundleVersionManagementService bundleVersionManagementService,
                                     ElementDefinitionService elementDefinitionService,
                                     PackageManagementService packageManagementService,
                                     ExportManifestService exportManifestService) {
        this.bundleManagementService = bundleManagementService;
        this.bundleVersionManagementService = bundleVersionManagementService;
        this.elementDefinitionService = elementDefinitionService;
        this.packageManagementService = packageManagementService;
        this.exportManifestService = exportManifestService;
    }

    @Override
    public Object getBundle(String fqn) {
        return bundleManagementService.findByFqn(fqn).orElse(null);
    }

    @Override
    public PageResult<?> listBundles(PageRequest pageRequest) {
        List<?> list = bundleManagementService.listAll();
        return new PageResult<>(list, list.size(), pageRequest.getPage(), pageRequest.getSize());
    }

    @Override
    public Object getEntitySchema(String fqn) {
        return elementDefinitionService.findEntitySchemaByFqn(fqn).orElse(null);
    }

    @Override
    public PageResult<?> listEntitySchemas(Object query) {
        ElementQueryRequest eqr = (ElementQueryRequest) query;
        List<?> list = elementDefinitionService.listEntitySchemas(eqr);
        return new PageResult<>(list, list.size(), eqr.getPage(), eqr.getSize());
    }

    @Override
    public Object getRelationSchema(String fqn) {
        return elementDefinitionService.findRelationSchemaByFqn(fqn).orElse(null);
    }

    @Override
    public PageResult<?> listRelationSchemas(Object query) {
        ElementQueryRequest eqr = (ElementQueryRequest) query;
        List<?> list = elementDefinitionService.listRelationSchemas(eqr);
        return new PageResult<>(list, list.size(), eqr.getPage(), eqr.getSize());
    }

    @Override
    public List<?> listPackages(String bundleVersionFqn) {
        return packageManagementService.listByBundleVersion(bundleVersionFqn);
    }

    @Override
    public Object getExport(String bundleVersionFqn) {
        return exportManifestService.findByVersionFqn(bundleVersionFqn).orElse(null);
    }

    @Override
    public boolean isPackageExported(String bundleVersionFqn, String packageFqn) {
        return exportManifestService.findByVersionFqn(bundleVersionFqn)
                .map(m -> {
                    try {
                        @SuppressWarnings("unchecked")
                        List<String> packages = (List<String>) m.getClass()
                                .getMethod("getPackages").invoke(m);
                        return packages != null && packages.contains(packageFqn);
                    } catch (Exception e) {
                        return true;
                    }
                }).orElse(false);
    }

    @Override
    public Object getDependencyGraph(String bundleFqn) {
        return null;
    }

    @Override
    public List<?> listBundleVersions(String bundleFqn) {
        return bundleVersionManagementService.listByBundle(bundleFqn);
    }
}
