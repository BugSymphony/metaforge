package com.metaforge.agent.cognition.core.infrastructure.adapter;

import com.metaforge.agent.cognition.core.domain.port.MetamodelClientPort;
import com.metaforge.metamodel.api.service.BundleManagementService;
import com.metaforge.metamodel.api.service.BundleVersionManagementService;
import com.metaforge.metamodel.api.service.ElementDefinitionService;
import com.metaforge.metamodel.api.dto.ElementQueryRequest;
import com.metaforge.metamodel.api.dto.response.BundleDto;
import com.metaforge.metamodel.api.dto.response.BundleVersionDto;
import com.metaforge.metamodel.api.dto.response.EntitySchemaDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class MetamodelClientAdapter implements MetamodelClientPort {

    private static final Logger log = LoggerFactory.getLogger(MetamodelClientAdapter.class);

    private final BundleManagementService bundleManagementService;
    private final BundleVersionManagementService bundleVersionManagementService;
    private final ElementDefinitionService elementDefinitionService;

    public MetamodelClientAdapter(BundleManagementService bundleManagementService,
                                   BundleVersionManagementService bundleVersionManagementService,
                                   ElementDefinitionService elementDefinitionService) {
        this.bundleManagementService = bundleManagementService;
        this.bundleVersionManagementService = bundleVersionManagementService;
        this.elementDefinitionService = elementDefinitionService;
    }

    @Override
    public Object getBundle(String fqn) {
        Optional<BundleDto> bundle = bundleManagementService.findByFqn(fqn);
        bundle.ifPresent(b -> log.debug("查询 Bundle 成功: fqn={}", fqn));
        return bundle.orElse(null);
    }

    @Override
    public Object listBundles(int page, int size) {
        List<BundleDto> bundles = bundleManagementService.listAll();
        log.debug("列举 Bundle: count={}", bundles.size());
        return bundles;
    }

    @Override
    public String getLatestPublishedVersion(String bundleFqn) {
        List<BundleVersionDto> versions = bundleVersionManagementService.listByBundle(bundleFqn);
        BundleVersionDto published = versions.stream()
                .filter(v -> "PUBLISHED".equals(v.getStatus()))
                .findFirst()
                .orElse(null);
        log.debug("查询最新已发布版本: bundleFqn={}, version={}",
                bundleFqn, published != null ? published.getFqn() : "none");
        return published != null ? published.getFqn() : null;
    }

    @Override
    public Object getEntitySchema(String fqn) {
        Optional<EntitySchemaDto> schema = elementDefinitionService.findEntitySchemaByFqn(fqn);
        schema.ifPresent(s -> log.debug("查询 EntitySchema: fqn={}", fqn));
        return schema.orElse(null);
    }

    @Override
    public Object listEntitySchemasByPrefixes(List<String> fqnPrefixes) {
        ElementQueryRequest request = new ElementQueryRequest();
        request.setFqnPrefixes(fqnPrefixes);
        request.setPage(1);
        request.setSize(Integer.MAX_VALUE);

        List<EntitySchemaDto> schemas = elementDefinitionService.listEntitySchemas(request);
        log.debug("按前缀查询 EntitySchema: prefixes={}, count={}", fqnPrefixes, schemas.size());
        return schemas;
    }

    @Override
    public String resolveBundleFqnByPrefix(String fqnPrefix) {
        List<BundleDto> allBundles = bundleManagementService.listAll();
        for (BundleDto bundle : allBundles) {
            if (bundle.getFqn() != null && bundle.getFqn().startsWith(fqnPrefix)) {
                log.debug("按前缀解析 Bundle FQN: prefix={}, resolved={}", fqnPrefix, bundle.getFqn());
                return bundle.getFqn();
            }
        }
        log.debug("按前缀解析 Bundle FQN 失败，无匹配 Bundle: prefix={}", fqnPrefix);
        return null;
    }
}
