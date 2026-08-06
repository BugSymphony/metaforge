package com.metaforge.metamodel.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.metaforge.metamodel.domain.model.entity.AttributeTemplate;
import com.metaforge.metamodel.domain.model.valueobject.Fqn;
import com.metaforge.metamodel.infrastructure.persistence.jpa.AttributeTemplateJpo;

/**
 * AttributeTemplate MapStruct 转换器 — 领域对象 ↔ JPA 持久化对象。
 */
@Mapper(componentModel = "spring")
public interface AttributeTemplateMapper {

    @Mapping(target = "fqn", source = "fqnValue")
    AttributeTemplateJpo toJpo(AttributeTemplate entity);

    @Mapping(target = "fqn", expression = "java(toFqn(jpo.getFqn()))")
    AttributeTemplate toDomain(AttributeTemplateJpo jpo);

    default Fqn toFqn(String fqn) {
        return fqn != null ? Fqn.of(fqn) : null;
    }
}
