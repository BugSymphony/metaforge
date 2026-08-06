package com.metaforge.agent.cognition.core.infrastructure.mapper;

import com.metaforge.agent.cognition.api.dto.request.CognitionRequest;
import com.metaforge.agent.cognition.api.dto.request.CognitionRequest;
import com.metaforge.agent.cognition.core.domain.model.valueobject.QueryParameters;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

@Mapper(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CognitionMapper {

    CognitionMapper INSTANCE = Mappers.getMapper(CognitionMapper.class);

    @Mapping(target = "contextParameters", source = "contextParameters")
    QueryParameters toQueryParameters(CognitionRequest request);
}
