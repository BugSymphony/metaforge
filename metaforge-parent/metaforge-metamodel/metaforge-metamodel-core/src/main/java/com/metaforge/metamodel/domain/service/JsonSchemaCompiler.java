package com.metaforge.metamodel.domain.service;

import com.metaforge.common.util.JsonbUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JSON Schema 编译器领域服务。
 * <p>将合并后的属性定义编译为符合 JSON Schema Draft 2020-12 规范的扁平输出：
 * properties 为 {属性名: 属性Schema} 映射，required 为必填属性名列表，
 * 并映射 type 与 pattern/minimum/format 等约束关键字。</p>
 */
@Component
public class JsonSchemaCompiler {

    private static final String JSON_SCHEMA_URI = "https://json-schema.org/draft/2020-12/schema";

    private static final Set<String> CONSTRAINT_KEYWORDS = Set.of(
            "pattern", "minimum", "maximum", "exclusiveMinimum", "exclusiveMaximum",
            "minLength", "maxLength", "multipleOf", "format", "enum",
            "minItems", "maxItems", "minProperties", "maxProperties");

    /**
     * 将属性定义 JSON 编译为 JSON Schema Draft 2020-12 格式。
     *
     * @param mergedAttributesJson 合并后的属性定义 JSON 字符串（JSON 数组）
     * @param entityName           实体名称（用于 Schema title）
     * @return JSON Schema 字符串
     */
    public String compile(String mergedAttributesJson, String entityName) {
        List<Map<String, Object>> attributes = parseAttributes(mergedAttributesJson);

        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();

        if (attributes != null) {
            for (Map<String, Object> attr : attributes) {
                String name = asString(attr.get("name"));
                if (name == null || name.isBlank()) {
                    continue;
                }
                properties.put(name, buildPropertySchema(attr));
                if (Boolean.TRUE.equals(attr.get("required"))) {
                    required.add(name);
                }
            }
        }

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("$schema", JSON_SCHEMA_URI);
        if (entityName != null && !entityName.isBlank()) {
            schema.put("title", entityName);
        }
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);

        return JsonbUtils.toJsonb(schema);
    }

    private Map<String, Object> buildPropertySchema(Map<String, Object> attr) {
        Map<String, Object> propertySchema = new LinkedHashMap<>();

        Object type = attr.get("type");
        if (type != null) {
            propertySchema.put("type", type);
        }

        Object description = attr.get("description");
        if (description != null) {
            propertySchema.put("description", description);
        }

        // 约束既可嵌套在 constraints 字段，也可平铺在属性定义顶层
        Object constraints = attr.get("constraints");
        if (constraints instanceof Map<?, ?> constraintsMap) {
            for (Map.Entry<?, ?> entry : constraintsMap.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (CONSTRAINT_KEYWORDS.contains(key)) {
                    propertySchema.put(key, entry.getValue());
                }
            }
        }
        for (Map.Entry<String, Object> entry : attr.entrySet()) {
            if (CONSTRAINT_KEYWORDS.contains(entry.getKey())) {
                propertySchema.put(entry.getKey(), entry.getValue());
            }
        }

        return propertySchema;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseAttributes(String mergedAttributesJson) {
        if (mergedAttributesJson == null || mergedAttributesJson.isBlank()) {
            return List.of();
        }
        String trimmed = mergedAttributesJson.trim();
        if (!trimmed.startsWith("[")) {
            return List.of();
        }
        try {
            return JsonbUtils.fromJsonb(trimmed, List.class);
        } catch (Exception e) {
            return List.of();
        }
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
