package com.metaforge.agent.cognition.core.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface UpstreamDtoMapper {

    UpstreamDtoMapper INSTANCE = Mappers.getMapper(UpstreamDtoMapper.class);
}
