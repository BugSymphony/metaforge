package com.metaforge.metamodel.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.metaforge.metamodel.domain.model.aggregate.Bundle;
import com.metaforge.metamodel.domain.model.valueobject.Fqn;
import com.metaforge.metamodel.infrastructure.persistence.jpa.BundleJpo;

/**
 * Bundle MapStruct 转换器 — 领域对象 ↔ JPA 持久化对象。
 */
@Mapper(componentModel = "spring")
public interface BundleMapper {

    @Mapping(target = "fqn", source = "fqnValue")
    @Mapping(target = "isSystem", source = "system")
    BundleJpo toJpo(Bundle bundle);

    @Mapping(target = "fqn", expression = "java(toFqn(jpo.getFqn()))")
    @Mapping(target = "system", source = "isSystem")
    Bundle toDomain(BundleJpo jpo);

    default Fqn toFqn(String fqn) {
        return fqn != null ? Fqn.of(fqn) : null;
    }
}
