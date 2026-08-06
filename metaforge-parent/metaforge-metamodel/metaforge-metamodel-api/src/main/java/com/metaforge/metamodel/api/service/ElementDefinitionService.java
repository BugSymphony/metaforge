package com.metaforge.metamodel.api.service;

import com.metaforge.metamodel.api.dto.request.*;
import com.metaforge.metamodel.api.dto.response.*;
import com.metaforge.metamodel.api.dto.ElementQueryRequest;

import java.util.List;
import java.util.Optional;

/**
 * 元素定义应用服务接口（契约层）。
 */
public interface ElementDefinitionService {

    // EntitySchema
    EntitySchemaDto createEntitySchema(CreateEntitySchemaRequest request);
    Optional<EntitySchemaDto> findEntitySchemaByFqn(String fqn);
    EntitySchemaDto updateEntitySchema(String fqn, UpdateEntitySchemaRequest request);
    void deleteEntitySchema(String fqn);
    List<EntitySchemaDto> listEntitySchemas(ElementQueryRequest request);

    // RelationSchema
    RelationSchemaDto createRelationSchema(CreateRelationSchemaRequest request);
    Optional<RelationSchemaDto> findRelationSchemaByFqn(String fqn);
    void deleteRelationSchema(String fqn);
    List<RelationSchemaDto> listRelationSchemas(ElementQueryRequest request);

    // AttributeTemplate
    AttributeTemplateDto createAttributeTemplate(CreateAttributeTemplateRequest request);
    Optional<AttributeTemplateDto> findAttributeTemplateByFqn(String fqn);
    void deleteAttributeTemplate(String fqn);
}
