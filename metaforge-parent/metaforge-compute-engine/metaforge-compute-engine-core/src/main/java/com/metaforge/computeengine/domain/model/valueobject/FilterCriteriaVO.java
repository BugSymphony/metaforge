package com.metaforge.computeengine.domain.model.valueobject;

import com.metaforge.computeengine.api.dto.common.FilterCriteria.FqnFilterGroup;
import com.metaforge.computeengine.api.dto.common.FilterCriteria.PropertyFilter;
import com.metaforge.computeengine.api.enums.AssociationType;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 领域层过滤条件值对象。
 *
 * <p>对应 API 层 FilterCriteria DTO。维度间 AND 逻辑，维度内 OR 逻辑。
 * 评估方法可判断给定的实体/关系是否通过过滤条件。
 *
 * @author metaforge
 */
public final class FilterCriteriaVO {

    private final Set<AssociationType> associationTypes;
    private final List<FqnFilterGroup> sourceFqns;
    private final List<FqnFilterGroup> targetFqns;
    private final List<FqnFilterGroup> relationInstanceFqns;
    private final List<FqnFilterGroup> entityTypes;
    private final List<FqnFilterGroup> relationTypes;
    private final List<PropertyFilter> propertyFilters;

    public FilterCriteriaVO(Set<AssociationType> associationTypes, List<FqnFilterGroup> sourceFqns,
                            List<FqnFilterGroup> targetFqns, List<FqnFilterGroup> relationInstanceFqns,
                            List<FqnFilterGroup> entityTypes, List<FqnFilterGroup> relationTypes,
                            List<PropertyFilter> propertyFilters) {
        this.associationTypes = associationTypes != null ? Collections.unmodifiableSet(associationTypes) : Collections.emptySet();
        this.sourceFqns = nullSafeList(sourceFqns);
        this.targetFqns = nullSafeList(targetFqns);
        this.relationInstanceFqns = nullSafeList(relationInstanceFqns);
        this.entityTypes = nullSafeList(entityTypes);
        this.relationTypes = nullSafeList(relationTypes);
        this.propertyFilters = nullSafeList(propertyFilters);
    }

    public Set<AssociationType> getAssociationTypes() {
        return associationTypes;
    }

    public List<FqnFilterGroup> getSourceFqns() {
        return sourceFqns;
    }

    public List<FqnFilterGroup> getTargetFqns() {
        return targetFqns;
    }

    public List<FqnFilterGroup> getRelationInstanceFqns() {
        return relationInstanceFqns;
    }

    public List<FqnFilterGroup> getEntityTypes() {
        return entityTypes;
    }

    public List<FqnFilterGroup> getRelationTypes() {
        return relationTypes;
    }

    public List<PropertyFilter> getPropertyFilters() {
        return propertyFilters;
    }

    public boolean isEmpty() {
        return associationTypes.isEmpty() && sourceFqns.isEmpty() && targetFqns.isEmpty()
                && relationInstanceFqns.isEmpty() && entityTypes.isEmpty()
                && relationTypes.isEmpty() && propertyFilters.isEmpty();
    }

    public boolean hasAssociationTypes() {
        return !associationTypes.isEmpty();
    }

    public boolean hasSourceFqns() {
        return !sourceFqns.isEmpty();
    }

    public boolean hasTargetFqns() {
        return !targetFqns.isEmpty();
    }

    public boolean hasRelationInstanceFqns() {
        return !relationInstanceFqns.isEmpty();
    }

    public boolean hasEntityTypes() {
        return !entityTypes.isEmpty();
    }

    public boolean hasRelationTypes() {
        return !relationTypes.isEmpty();
    }

    public boolean hasPropertyFilters() {
        return !propertyFilters.isEmpty();
    }

    private static <T> List<T> nullSafeList(List<T> list) {
        return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
    }
}
