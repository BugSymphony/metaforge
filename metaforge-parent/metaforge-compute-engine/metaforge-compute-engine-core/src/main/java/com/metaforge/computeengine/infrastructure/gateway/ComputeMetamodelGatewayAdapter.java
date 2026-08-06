package com.metaforge.computeengine.infrastructure.gateway;

import com.metaforge.computeengine.domain.port.MetamodelSemanticPort;
import com.metaforge.metamodel.api.service.ElementDefinitionService;
import org.springframework.stereotype.Component;

/**
 * 元模型 API 网关适配器。
 *
 * <p>实现 MetamodelSemanticPort，委托调用上游 metaforge-metamodel-api 的 ElementDefinitionService。
 * 仅访问已发布版本的元模型定义，用于校验 EntitySchema/RelationSchema 是否存在及其定义。
 *
 * @author metaforge
 */
@Component
public class ComputeMetamodelGatewayAdapter implements MetamodelSemanticPort {

    private final ElementDefinitionService elementDefinitionService;

    public ComputeMetamodelGatewayAdapter(ElementDefinitionService elementDefinitionService) {
        this.elementDefinitionService = elementDefinitionService;
    }

    @Override
    public Object getEntitySchema(String fqn) {
        return elementDefinitionService.findEntitySchemaByFqn(fqn).orElse(null);
    }

    @Override
    public Object getRelationSchema(String fqn) {
        return elementDefinitionService.findRelationSchemaByFqn(fqn).orElse(null);
    }

    @Override
    public boolean isEntitySchemaExists(String fqn) {
        return elementDefinitionService.findEntitySchemaByFqn(fqn).isPresent();
    }

    @Override
    public boolean isRelationSchemaExists(String fqn) {
        return elementDefinitionService.findRelationSchemaByFqn(fqn).isPresent();
    }
}
