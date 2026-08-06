package com.metaforge.graph.domain.repository;

import com.metaforge.graph.domain.model.valueobject.CardinalityRule;

/**
 * 上游元模型访问领域端口接口。
 * 通过 metamodel-governance BC 的 api 模块获取 RelationSchema 信息。
 */
public interface RelationSchemaRepository {

    /**
     * 获取 RelationSchema 的 JSON Schema 字符串。
     *
     * @param relationSchemaFqn RelationSchema FQN
     * @return JSON Schema 字符串
     */
    String getRelationSchemaSchema(String relationSchemaFqn);

    /**
     * 判断 RelationSchema 版本是否已发布。
     *
     * @param relationSchemaFqn RelationSchema FQN
     * @return true 表示已发布
     */
    boolean isSchemaPublished(String relationSchemaFqn);

    /**
     * 获取 RelationSchema 的基数约束规则。
     *
     * @param relationSchemaFqn RelationSchema FQN
     * @return CardinalityRule
     */
    CardinalityRule getCardinalityRule(String relationSchemaFqn);
}
