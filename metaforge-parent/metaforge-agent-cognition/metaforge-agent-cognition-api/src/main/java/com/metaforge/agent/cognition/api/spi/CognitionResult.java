package com.metaforge.agent.cognition.api.spi;

import com.metaforge.agent.cognition.api.enums.DimensionCategory;

/**
 * 算子执行结果——组件顺序即序列化顺序：
 * operatorId, name, category, description, data, success, error。
 */
public record CognitionResult(
        String operatorId,
        String name,
        DimensionCategory category,
        String description,
        Object data,
        boolean success,
        String error
) {
    public static CognitionResult success(String operatorId, DimensionCategory category, Object data) {
        return new CognitionResult(operatorId, null, category, null, data, true, null);
    }

    public static CognitionResult failure(String operatorId, DimensionCategory category, String error) {
        return new CognitionResult(operatorId, null, category, null, null, false, error);
    }

    public static CognitionResult successWithMeta(String operatorId, String name, DimensionCategory category,
                                                  String description, Object data) {
        return new CognitionResult(operatorId, name, category, description, data, true, null);
    }
}
