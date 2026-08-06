package com.metaforge.graph.infrastructure.persistence.jpa;

import com.metaforge.common.util.JsonbUtils;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;

/**
 * embedding 字段 JSONB 转换器。
 * 在 JPA 持久化时将 List<Float> 序列化为 JSON 字符串，读取时反序列化。
 */
@Converter
public class EmbeddingJsonbConverter implements AttributeConverter<List<Float>, String> {

    @Override
    public String convertToDatabaseColumn(List<Float> attribute) {
        if (attribute == null) {
            return null;
        }
        return JsonbUtils.toJsonb(attribute);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Float> convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return JsonbUtils.fromJsonb(dbData, List.class);
    }
}
