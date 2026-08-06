package com.metaforge.metadata.infrastructure.mapper;

import com.metaforge.common.util.JsonbUtils;
import com.metaforge.metadata.domain.model.aggregate.MetadataEntity;
import com.metaforge.metadata.domain.model.valueobject.EntitySchemaFQN;
import com.metaforge.metadata.domain.model.valueobject.FQN;
import com.metaforge.metadata.domain.model.valueobject.VersionNumber;
import com.metaforge.metadata.infrastructure.persistence.jpa.MetadataEntityJpo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface MetadataEntityMapper {

    @Mapping(target = "fqn", source = "fqnValue")
    @Mapping(target = "entitySchemaFqn", source = "entitySchemaFqnValue")
    @Mapping(target = "currentVersion", source = "currentVersionValue")
    @Mapping(target = "content", source = "content", qualifiedByName = "mapToString")
    @Mapping(target = "embedding", ignore = true)
    MetadataEntityJpo toJpo(MetadataEntity entity);

    @Mapping(target = "fqn", expression = "java(toFqn(jpo.getFqn()))")
    @Mapping(target = "entitySchemaFqn", expression = "java(toEntitySchemaFqn(jpo.getEntitySchemaFqn()))")
    @Mapping(target = "currentVersion", expression = "java(toVersionNumber(jpo.getCurrentVersion()))")
    @Mapping(target = "content", source = "content", qualifiedByName = "stringToMap")
    @Mapping(target = "embedding", ignore = true)
    MetadataEntity toDomain(MetadataEntityJpo jpo);

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
    default VersionNumber toVersionNumber(Integer version) { return version != null ? VersionNumber.of(version) : null; }
}
