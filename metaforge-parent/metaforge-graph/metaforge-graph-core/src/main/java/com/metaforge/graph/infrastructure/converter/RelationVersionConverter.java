package com.metaforge.graph.infrastructure.converter;

import com.metaforge.graph.domain.model.aggregate.RelationVersion;
import com.metaforge.graph.infrastructure.persistence.jpa.RelationVersionJpo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * RelationVersion MapStruct 转换器 — 领域对象 ↔ JPA 持久化对象。
 */
@Mapper(componentModel = "spring")
public interface RelationVersionConverter {

    RelationVersionJpo toJpo(RelationVersion version);

    RelationVersion toDomain(RelationVersionJpo jpo);
}
