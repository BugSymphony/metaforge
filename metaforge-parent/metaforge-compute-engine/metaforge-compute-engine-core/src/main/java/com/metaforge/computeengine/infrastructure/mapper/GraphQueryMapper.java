package com.metaforge.computeengine.infrastructure.mapper;

import com.metaforge.computeengine.api.dto.common.EntitySummary;
import com.metaforge.computeengine.api.dto.common.RelationSummary;
import com.metaforge.computeengine.api.dto.response.GraphQueryResult;
import com.metaforge.computeengine.api.dto.response.PathResult;
import com.metaforge.computeengine.domain.model.aggregate.GraphQuery;
import com.metaforge.computeengine.domain.model.entity.TraversalPath;
import com.metaforge.computeengine.domain.model.valueobject.EntitySnapshot;
import com.metaforge.computeengine.domain.model.valueobject.RelationSnapshot;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueMappingStrategy;

/**
 * 图查询聚合根 → DTO 的 MapStruct 转换器。
 *
 * @author metaforge */
@Mapper(componentModel = "spring", nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
public interface GraphQueryMapper {

    /**
     * EntitySnapshot → EntitySummary。
     */
    default EntitySummary toSummary(EntitySnapshot snapshot) {
        if (snapshot == null) return null;
        return new EntitySummary(
                snapshot.getFqn().getValue(),
                snapshot.getName(),
                snapshot.getEntitySchemaFqn() != null ? snapshot.getEntitySchemaFqn().getValue() : null
        );
    }

    /**
     * RelationSnapshot → RelationSummary。
     */
    default RelationSummary toSummary(RelationSnapshot snapshot) {
        if (snapshot == null) return null;
        return new RelationSummary(
                snapshot.getFqn().getValue(),
                snapshot.getAssociationType(),
                snapshot.getSourceEntityFqn() != null ? snapshot.getSourceEntityFqn().getValue() : null,
                snapshot.getTargetEntityFqn() != null ? snapshot.getTargetEntityFqn().getValue() : null
        );
    }
}
