package com.metaforge.graph.domain.service;

import java.util.Map;

/**
 * JSON Schema 结构校验领域服务。
 * 基于 RelationSchemaFQN 获取对应 JSON Schema 并校验 content。
 */
public interface RelationSchemaValidationService {

    /**
     * 校验 content 是否符合对应 RelationSchema 的 JSON Schema 结构。
     *
     * @param relationSchemaFqn RelationSchema FQN（含版本号）
     * @param content           待校验的属性内容
     * @throws SchemaValidationException 如果校验失败
     */
    void validate(String relationSchemaFqn, Map<String, Object> content);
}
