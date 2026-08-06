package com.metaforge.metadata.infrastructure.mapper;

import com.metaforge.common.util.JsonbUtils;
import com.metaforge.metadata.domain.model.aggregate.MetadataEntityDraft;
import com.metaforge.metadata.domain.model.valueobject.EntitySchemaFQN;
import com.metaforge.metadata.domain.model.valueobject.FQN;
import com.metaforge.metadata.infrastructure.persistence.jpa.MetadataEntityDraftJpo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface MetadataDraftMapper {

    @Mapping(target = "fqn", source = "fqnValue")
    @Mapping(target = "entitySchemaFqn", source = "entitySchemaFqnValue")
    @Mapping(target = "content", source = "content", qualifiedByName = "mapToString")
    @Mapping(target = "embedding", ignore = true)
    MetadataEntityDraftJpo toJpo(MetadataEntityDraft draft);

    @Mapping(target = "fqn", expression = "java(toFqn(jpo.getFqn()))")
    @Mapping(target = "entitySchemaFqn", expression = "java(toEntitySchemaFqn(jpo.getEntitySchemaFqn()))")
    @Mapping(target = "content", source = "content", qualifiedByName = "stringToMap")
    @Mapping(target = "embedding", ignore = true)
    MetadataEntityDraft toDomain(MetadataEntityDraftJpo jpo);

    @Named("mapToString")
    default String mapToString(Map<String, Object> content) {
        return content != null ? JsonbUtils.toJsonb(content) : null;
    }

    @Named("stringToMap")
    default Map<String, Object> stringToMap(String content) {
        if (content == null || content.isEmpty()) return Collections.emptyMap();
        return JsonbUtils.fromJsonb(content, Map.class);
    }

    default FQN toFqn(String fqn) { return fqn != null ? FQN.of(fqn) : null; }
    default EntitySchemaFQN toEntitySchemaFqn(String fqn) { return fqn != null ? EntitySchemaFQN.of(fqn) : null; }
}
