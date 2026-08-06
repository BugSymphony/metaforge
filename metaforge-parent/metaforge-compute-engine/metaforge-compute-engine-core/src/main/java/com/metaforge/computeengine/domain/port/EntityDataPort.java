package com.metaforge.computeengine.domain.port;

import com.metaforge.computeengine.domain.model.valueobject.EntitySnapshot;
import com.metaforge.computeengine.domain.model.valueobject.FQN;

import java.util.List;

/**
 * 实体数据查询端口。
 *
 * <p>领域层定义的只读查询接口，由基础设施层通过 jOOQ 适配器实现。
 * 所有查询基于生效态数据（STATUS='ACTIVE'）。
 *
 * @author metaforge
 */
public interface EntityDataPort {

    /**
     * 按 FQN 查找单个实体。
     *
     * @param fqn 实体 FQN
     * @return 实体快照，不存在时返回 null
     */
    EntitySnapshot findByFqn(FQN fqn);

    /**
     * 按 FQN 前缀批量查找实体。
     *
     * @param fqnPrefixes FQN 前缀列表
     * @param limit       最大返回数
     * @return 匹配的实体快照列表
     */
    List<EntitySnapshot> findByFqnPrefixes(List<String> fqnPrefixes, int limit);

    /**
     * 按 EntitySchema FQN 查找该类型下的所有实体。
     *
     * @param entitySchemaFqn EntitySchema FQN
     * @param limit           最大返回数
     * @return 匹配的实体快照列表
     */
    List<EntitySnapshot> findByEntitySchemaFqn(String entitySchemaFqn, int limit);

    /**
     * 按 FQN 列表批量查找实体。
     *
     * @param fqns FQN 列表
     * @return 存在的实体快照列表（不存在的 FQN 不在结果中）
     */
    List<EntitySnapshot> batchFindByFqns(List<FQN> fqns);
}
