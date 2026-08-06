package com.metaforge.computeengine.domain.port;

/**
 * 元模型语义查询端口。
 *
 * <p>领域层定义的元模型结构查询接口，由基础设施层通过上游 Metamodel API 适配器实现。
 * 仅获取 EntitySchema/RelationSchema 的已发布版本定义。
 *
 * @author metaforge
 */
public interface MetamodelSemanticPort {

    /**
     * 获取 EntitySchema 定义。
     *
     * @param fqn EntitySchema FQN
     * @return EntitySchema 信息对象，不存在时返回 null
     */
    Object getEntitySchema(String fqn);

    /**
     * 获取 RelationSchema 定义。
     *
     * @param fqn RelationSchema FQN
     * @return RelationSchema 信息对象，不存在时返回 null
     */
    Object getRelationSchema(String fqn);

    /**
     * 判断指定 FQN 的 EntitySchema 是否存在。
     *
     * @param fqn EntitySchema FQN
     * @return 存在返回 true
     */
    boolean isEntitySchemaExists(String fqn);

    /**
     * 判断指定 FQN 的 RelationSchema 是否存在。
     *
     * @param fqn RelationSchema FQN
     * @return 存在返回 true
     */
    boolean isRelationSchemaExists(String fqn);
}
