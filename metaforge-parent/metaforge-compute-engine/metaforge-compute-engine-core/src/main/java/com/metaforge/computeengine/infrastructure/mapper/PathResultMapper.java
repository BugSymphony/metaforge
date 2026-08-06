package com.metaforge.computeengine.infrastructure.mapper;

import com.metaforge.computeengine.api.dto.response.ClosureResult;
import com.metaforge.computeengine.api.dto.response.PathResult;
import com.metaforge.computeengine.domain.model.aggregate.PathQuery;
import com.metaforge.computeengine.domain.model.entity.ClosuredEntity;
import com.metaforge.computeengine.domain.model.entity.TraversalPath;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueMappingStrategy;

/**
 * 路径推理结果 MapStruct 转换器。
 *
 * @author metaforge
 */
@Mapper(componentModel = "spring", nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
public interface PathResultMapper {

    /**
     * ClosureResult 按层级分组映射。
     */
    default java.util.Map<Integer, java.util.List<ClosureResult.ClosuredEntityDetail>> mapClosureLayers(
            java.util.Map<Integer, java.util.List<ClosuredEntity>> layers) {
        if (layers == null) return null;
        java.util.Map<Integer, java.util.List<ClosureResult.ClosuredEntityDetail>> result = new java.util.LinkedHashMap<>();
        for (var entry : layers.entrySet()) {
            result.put(entry.getKey(), entry.getValue().stream()
                    .map(this::toDetail).toList());
        }
        return result;
    }

    default ClosureResult.ClosuredEntityDetail toDetail(ClosuredEntity entity) {
        if (entity == null) return null;
        return new ClosureResult.ClosuredEntityDetail(
                entity.getFqn().getValue(), entity.getDepth(), entity.getArrivedByTypes());
    }
}
