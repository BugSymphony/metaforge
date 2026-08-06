package com.metaforge.graph.infrastructure.persistence.adapter;

import com.metaforge.graph.api.constant.GraphErrorCode;
import com.metaforge.graph.infrastructure.config.GraphBizException;
import com.metaforge.graph.domain.model.valueobject.CardinalityRule;
import com.metaforge.graph.domain.repository.RelationSchemaRepository;
import com.metaforge.metamodel.api.dto.response.RelationSchemaDto;
import com.metaforge.metamodel.api.service.ElementDefinitionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 元模型访问适配器——通过 metaforge-metamodel-api 的 ElementDefinitionService 消费上游服务。
 *
 * <p>实现 {@link RelationSchemaRepository} 领域端口，将上游 metamodel-governance BC
 * 的 RelationSchema 查询能力适配为本 BC 领域层可消费的接口。
 */
@Component
public class GraphMetamodelGatewayAdapter implements RelationSchemaRepository {

    private static final Logger log = LoggerFactory.getLogger(GraphMetamodelGatewayAdapter.class);

    private final ElementDefinitionService elementDefinitionService;

    public GraphMetamodelGatewayAdapter(ElementDefinitionService elementDefinitionService) {
        this.elementDefinitionService = elementDefinitionService;
    }

    @Override
    public String getRelationSchemaSchema(String relationSchemaFqn) {
        log.debug("查询 RelationSchema JSON Schema: fqn={}", relationSchemaFqn);

        Optional<RelationSchemaDto> schemaOpt = elementDefinitionService.findRelationSchemaByFqn(relationSchemaFqn);
        if (schemaOpt.isEmpty()) {
            throw new SchemaNotFoundException("RelationSchema 不存在: " + relationSchemaFqn);
        }

        RelationSchemaDto schema = schemaOpt.get();
        if (schema.getJsonSchema() == null || schema.getJsonSchema().isEmpty()) {
            log.debug("RelationSchema {} 无 JSON Schema 定义，返回空 Schema", relationSchemaFqn);
            return "{}";
        }

        return schema.getJsonSchema();
    }

    @Override
    public boolean isSchemaPublished(String relationSchemaFqn) {
        log.debug("检查 RelationSchema 是否已发布: fqn={}", relationSchemaFqn);

        Optional<RelationSchemaDto> schemaOpt = elementDefinitionService.findRelationSchemaByFqn(relationSchemaFqn);
        if (schemaOpt.isEmpty()) {
            throw new SchemaNotFoundException("RelationSchema 不存在: " + relationSchemaFqn);
        }

        return schemaOpt.get().isEnabled();
    }

    @Override
    public CardinalityRule getCardinalityRule(String relationSchemaFqn) {
        log.debug("获取 RelationSchema 基数约束: fqn={}", relationSchemaFqn);

        Optional<RelationSchemaDto> schemaOpt = elementDefinitionService.findRelationSchemaByFqn(relationSchemaFqn);
        if (schemaOpt.isEmpty()) {
            throw new SchemaNotFoundException("RelationSchema 不存在: " + relationSchemaFqn);
        }

        RelationSchemaDto schema = schemaOpt.get();
        return CardinalityRule.of(schema.getCardinalitySource(), schema.getCardinalityTarget());
    }

    public static class SchemaNotFoundException extends GraphBizException {
        public SchemaNotFoundException(String message) {
            super(GraphErrorCode.SCHEMA_NOT_PUBLISHED, message);
        }

        @Override
        public String getErrorCodeName() {
            return "SCHEMA_NOT_PUBLISHED";
        }
    }
}
