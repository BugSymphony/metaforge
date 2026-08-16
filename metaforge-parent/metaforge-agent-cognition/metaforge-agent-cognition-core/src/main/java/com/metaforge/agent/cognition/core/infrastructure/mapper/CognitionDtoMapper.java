package com.metaforge.agent.cognition.core.infrastructure.mapper;

import com.metaforge.agent.cognition.api.dto.request.CognitionRequest;
import com.metaforge.agent.cognition.api.dto.request.Scope;
import com.metaforge.agent.cognition.api.dto.response.CognitionResponse;
import com.metaforge.agent.cognition.api.dto.response.ContextMeta;
import com.metaforge.agent.cognition.api.spi.CognitionResult;
import com.metaforge.agent.cognition.core.domain.model.aggregate.CognitionQuery;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface CognitionDtoMapper {

    CognitionDtoMapper INSTANCE = Mappers.getMapper(CognitionDtoMapper.class);

    Scope mapScope(com.metaforge.agent.cognition.api.dto.request.Scope scope);

    default Scope mapScopeFromRequest(CognitionRequest request) {
        return request.scope();
    }

    default CognitionRequest mapRequest(CognitionRequest request) {
        return request;
    }

    default CognitionResponse mapResponse(CognitionQuery query, ContextMeta contextMeta,
                                           List<CognitionResult> dimensions, Object updatedScope) {
        Map<String, Object> scope = updatedScope instanceof Map<?, ?> m
                ? (Map<String, Object>) m : java.util.Collections.emptyMap();
        return new CognitionResponse(
                query.getTemplateId().value(),
                contextMeta,
                dimensions,
                query.getOutputFormat(),
                scope
        );
    }

    default Object mapResult(CognitionResult result) {
        return result.data();
    }
}
