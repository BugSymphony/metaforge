package com.metaforge.computeengine.infrastructure.mapper;

import com.metaforge.computeengine.api.dto.response.ImpactTraceResult;
import com.metaforge.computeengine.domain.model.aggregate.ImpactQuery;
import com.metaforge.computeengine.domain.model.entity.ImpactEntity;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueMappingStrategy;

/**
 * 影响溯源结果 MapStruct 转换器。
 *
 * @author metaforge */
@Mapper(componentModel = "spring", nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
public interface ImpactTraceMapper {

    default ImpactTraceResult.ImpactEntityDetail toDetail(ImpactEntity entity) {
        if (entity == null) return null;
        return new ImpactTraceResult.ImpactEntityDetail(
                entity.getFqn().getValue(), entity.getDepth(), entity.getAffectedByTypes());
    }
}
