package com.metaforge.graph.domain.repository;

/**
 * 上游元数据访问领域端口接口。
 * 通过 metadata-management BC 的 api 模块校验实体有效性。
 */
public interface MetadataEntityGateway {

    /**
     * 判断实体是否处于生效状态。
     *
     * @param entityFqn 实体 FQN
     * @return true 表示实体已生效
     */
    boolean isEntityActive(String entityFqn);

    /**
     * 获取实体信息。
     *
     * @param entityFqn 实体 FQN
     * @return 实体 FQN（若存在且生效），否则返回 null
     */
    String getEntityInfo(String entityFqn);
}
