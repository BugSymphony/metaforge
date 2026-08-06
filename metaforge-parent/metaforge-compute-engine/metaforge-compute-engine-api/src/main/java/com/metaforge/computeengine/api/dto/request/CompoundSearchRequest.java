package com.metaforge.computeengine.api.dto.request;

import com.metaforge.computeengine.api.dto.common.EntitySummary;
import com.metaforge.computeengine.api.enums.AssociationType;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.List;

/**
 * 多条件复合检索请求。
 *
 * <p>按实体类型、属性条件、关系条件组合过滤，支持分页与排序。
 *
 * @param entityTypes    实体类型（EntitySchema FQN）过滤
 * @param attributes     属性条件列表
 * @param relationTypes  关系类型过滤
 * @param page           页码（从 0 开始）
 * @param size           每页大小
 * @param sortField      排序字段
 * @param sortDirection  排序方向（ASC/DESC）
 * @author metaforge
 */
public record CompoundSearchRequest(
        List<String> entityTypes,
        List<AttributeCondition> attributes,
        List<AssociationType> relationTypes,
        int page,
        int size,
        String sortField,
        String sortDirection
) implements Serializable {

    /**
     * 属性条件。
     *
     * @param field    属性字段名
     * @param operator 操作符（EQ/NEQ/LIKE/GT/LT/GTE/LTE）
     * @param value    属性值
     */
    public record AttributeCondition(
            @NotNull String field,
            @NotNull String operator,
            @NotNull String value
    ) implements Serializable {
    }
}
