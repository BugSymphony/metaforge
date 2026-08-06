package com.metaforge.computeengine.domain.port;

import com.metaforge.computeengine.api.enums.AssociationType;
import com.metaforge.computeengine.api.enums.TraversalDirection;
import com.metaforge.computeengine.domain.model.valueobject.FQN;
import com.metaforge.computeengine.domain.model.valueobject.RelationSnapshot;

import java.util.List;
import java.util.Set;

/**
 * 关系数据查询端口。
 *
 * <p>领域层定义的只读查询接口，由基础设施层通过 jOOQ 适配器实现。
 * 所有查询基于生效态数据（STATUS='ACTIVE'）。
 *
 * @author metaforge
 */
public interface RelationDataPort {

    /**
     * 查找指定实体的出边关系。
     *
     * @param entityFqn 实体 FQN
     * @param types     关系类型过滤（空=全类型）
     * @param limit     最大返回数
     * @return 出边关系快照列表
     */
    List<RelationSnapshot> findOutboundRelations(FQN entityFqn, Set<AssociationType> types, int limit);

    /**
     * 查找指定实体的入边关系。
     *
     * @param entityFqn 实体 FQN
     * @param types     关系类型过滤（空=全类型）
     * @param limit     最大返回数
     * @return 入边关系快照列表
     */
    List<RelationSnapshot> findInboundRelations(FQN entityFqn, Set<AssociationType> types, int limit);

    /**
     * 按方向查找指定实体的关系。
     *
     * @param entityFqn 实体 FQN
     * @param direction 遍历方向（FORWARD=出边，BACKWARD=入边，BOTH=双向）
     * @param types     关系类型过滤（空=全类型）
     * @param limit     最大返回数
     * @return 关系快照列表
     */
    List<RelationSnapshot> findRelations(FQN entityFqn, TraversalDirection direction,
                                          Set<AssociationType> types, int limit);

    /**
     * 按 FQN 查找单个关系实例。
     *
     * @param relationFqn 关系实例 FQN
     * @return 关系快照，不存在时返回 null
     */
    RelationSnapshot findByFqn(FQN relationFqn);
}
