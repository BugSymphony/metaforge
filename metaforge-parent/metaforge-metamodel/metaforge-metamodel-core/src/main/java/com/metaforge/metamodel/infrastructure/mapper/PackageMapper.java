package com.metaforge.metamodel.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.metaforge.metamodel.domain.model.entity.Package;
import com.metaforge.metamodel.domain.model.valueobject.Fqn;
import com.metaforge.metamodel.infrastructure.persistence.jpa.PackageJpo;

/**
 * Package MapStruct 转换器 — 领域对象 ↔ JPA 持久化对象。
 */
@Mapper(componentModel = "spring")
public interface PackageMapper {

    @Mapping(target = "fqn", source = "fqnValue")
    PackageJpo toJpo(Package pkg);

    @Mapping(target = "fqn", expression = "java(toFqn(jpo.getFqn()))")
    Package toDomain(PackageJpo jpo);

    default Fqn toFqn(String fqn) {
        return fqn != null ? Fqn.of(fqn) : null;
    }
}
