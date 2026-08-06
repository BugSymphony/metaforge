package com.metaforge.metamodel.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.metaforge.metamodel.domain.model.entity.EntitySchema;
import com.metaforge.metamodel.domain.model.valueobject.Fqn;
import com.metaforge.metamodel.infrastructure.persistence.jpa.EntitySchemaJpo;

/**
 * EntitySchema MapStruct 转换器 — 领域对象 ↔ JPA 持久化对象。
 */
@Mapper(componentModel = "spring")
public interface EntitySchemaMapper {

    @Mapping(target = "fqn", source = "fqnValue")
    EntitySchemaJpo toJpo(EntitySchema entity);

    @Mapping(target = "fqn", expression = "java(toFqn(jpo.getFqn()))")
    EntitySchema toDomain(EntitySchemaJpo jpo);

    default Fqn toFqn(String fqn) {
        return fqn != null ? Fqn.of(fqn) : null;
    }
}
