package com.metaforge.graph.infrastructure.persistence.jpa;

import com.metaforge.common.util.JsonbUtils;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Map;

/**
 * content 字段 JSONB 转换器。
 * 在 JPA 持久化时将 Map 对象序列化为 JSON 字符串，读取时反序列化为 Map。
 */
@Converter
public class ContentJsonbConverter implements AttributeConverter<Map<String, Object>, String> {

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null) {
            return null;
        }
        return JsonbUtils.toJsonb(attribute);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return JsonbUtils.fromJsonb(dbData, Map.class);
    }
}
