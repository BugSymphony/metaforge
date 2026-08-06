package com.metaforge.graph.infrastructure.converter;

import com.metaforge.common.util.JsonbUtils;
import com.metaforge.graph.domain.model.aggregate.RelationInstanceDraft;
import com.metaforge.graph.domain.model.valueobject.*;
import com.metaforge.graph.infrastructure.persistence.jpa.RelationInstanceDraftJpo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.Map;

/**
 * RelationDraft MapStruct 转换器 — 领域对象 ↔ JPA 持久化对象。
 */
@Mapper(componentModel = "spring")
public interface RelationDraftConverter {

    @Mapping(target = "fqn", source = "fqnValue")
    @Mapping(target = "name", source = "nameValue")
    @Mapping(target = "description", source = "descriptionValue")
    @Mapping(target = "sourceEntityFqn", source = "sourceEntityFqnValue")
    @Mapping(target = "targetEntityFqn", source = "targetEntityFqnValue")
    @Mapping(target = "relationSchemaFqn", source = "relationSchemaFqnValue")
    @Mapping(target = "content", source = "content", qualifiedByName = "mapToString")
    @Mapping(target = "embedding", source = "embedding", qualifiedByName = "embeddingToString")
    RelationInstanceDraftJpo toJpo(RelationInstanceDraft draft);

    @Mapping(target = "fqn", source = "fqn", qualifiedByName = "stringToFqn")
    @Mapping(target = "name", source = "name", qualifiedByName = "stringToRelationName")
    @Mapping(target = "description", source = "description", qualifiedByName = "stringToDescription")
    @Mapping(target = "sourceEntityFqn", source = "sourceEntityFqn", qualifiedByName = "stringToEntityFqn")
    @Mapping(target = "targetEntityFqn", source = "targetEntityFqn", qualifiedByName = "stringToEntityFqn")
    @Mapping(target = "relationSchemaFqn", source = "relationSchemaFqn", qualifiedByName = "stringToSchemaFqn")
    @Mapping(target = "content", source = "content", qualifiedByName = "stringToMap")
    @Mapping(target = "embedding", ignore = true)
    RelationInstanceDraft toDomain(RelationInstanceDraftJpo jpo);

    @Named("mapToString")
    default String mapToString(Map<String, Object> content) {
        return content != null ? JsonbUtils.toJsonb(content) : null;
    }

    @Named("stringToMap")
    default Map<String, Object> stringToMap(String content) {
        if (content == null || content.isEmpty()) return Collections.emptyMap();
        return JsonbUtils.fromJsonb(content, Map.class);
    }

    @Named("embeddingToString")
    default String embeddingToString(java.util.List<Float> embedding) {
        return embedding != null ? JsonbUtils.toJsonb(embedding) : null;
    }

    @Named("stringToFqn")
    default FQN stringToFqn(String fqn) { return fqn != null ? FQN.of(fqn) : null; }

    @Named("stringToRelationName")
    default RelationName stringToRelationName(String name) { return name != null ? RelationName.of(name) : null; }

    @Named("stringToDescription")
    default RelationDescription stringToDescription(String desc) { return desc != null ? RelationDescription.of(desc) : null; }

    @Named("stringToEntityFqn")
    default EntityFQN stringToEntityFqn(String fqn) { return fqn != null ? EntityFQN.of(fqn) : null; }

    @Named("stringToSchemaFqn")
    default RelationSchemaFQN stringToSchemaFqn(String fqn) { return fqn != null ? RelationSchemaFQN.of(fqn) : null; }
}
