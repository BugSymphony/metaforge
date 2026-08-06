package com.metaforge.metamodel.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.metaforge.metamodel.api.enums.AssociationType;
import com.metaforge.metamodel.api.enums.Cardinality;
import com.metaforge.metamodel.domain.model.entity.RelationSchema;
import com.metaforge.metamodel.domain.model.valueobject.Fqn;
import com.metaforge.metamodel.infrastructure.persistence.jpa.RelationSchemaJpo;

/**
 * RelationSchema MapStruct 转换器 — 领域对象 ↔ JPA 持久化对象。
 */
@Mapper(componentModel = "spring")
public interface RelationSchemaMapper {

    @Mapping(target = "fqn", source = "fqnValue")
    @Mapping(target = "associationType", source = "associationType", qualifiedByName = "assocTypeToString")
    @Mapping(target = "cardinalitySource", source = "cardinalitySource", qualifiedByName = "cardToString")
    @Mapping(target = "cardinalityTarget", source = "cardinalityTarget", qualifiedByName = "cardToString")
    RelationSchemaJpo toJpo(RelationSchema entity);

    @Mapping(target = "fqn", expression = "java(toFqn(jpo.getFqn()))")
    @Mapping(target = "associationType", source = "associationType", qualifiedByName = "stringToAssocType")
    @Mapping(target = "cardinalitySource", source = "cardinalitySource", qualifiedByName = "stringToCardinality")
    @Mapping(target = "cardinalityTarget", source = "cardinalityTarget", qualifiedByName = "stringToCardinality")
    RelationSchema toDomain(RelationSchemaJpo jpo);

    default Fqn toFqn(String fqn) {
        return fqn != null ? Fqn.of(fqn) : null;
    }

    @Named("assocTypeToString")
    default String assocTypeToString(AssociationType type) {
        return type != null ? type.name() : null;
    }

    @Named("stringToAssocType")
    default AssociationType stringToAssocType(String type) {
        return type != null ? AssociationType.valueOf(type) : null;
    }

    @Named("cardToString")
    default String cardinalityToString(Cardinality card) {
        return card != null ? card.getNotation() : null;
    }

    @Named("stringToCardinality")
    default Cardinality stringToCardinality(String card) {
        return card != null ? Cardinality.fromNotation(card) : null;
    }
}
