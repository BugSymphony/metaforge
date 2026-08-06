package com.metaforge.graph.api.service;

import com.metaforge.graph.api.dto.DeactivationCheckResult;
import com.metaforge.graph.api.dto.RelationInstanceDto;

/**
 * 关系实例版本生效与生命周期管控服务。
 *
 * <p>生效操作为原子事务：同一事务内完成（1）主表写入/覆盖唯一生效版本、
 * （2）全量快照归档至历史表（版本号递增）、（3）删除草稿表对应记录、
 * （4）更新双向引用索引（源实体出边 + 目标实体入边）。
 * 任一步失败全量回滚，不存在中间脏状态。
 */
public interface RelationActivationService {

    /**
     * 对校验通过的草稿执行生效操作。
     *
     * @param fqn 草稿 FQN
     * @return 生效后的关系实例 DTO
     */
    RelationInstanceDto activate(String fqn);

    /**
     * 对生效关系执行下线操作。
     *
     * @param fqn 生效关系 FQN
     */
    void deprecate(String fqn);

    /**
     * 基于历史归档版本重新生效。
     *
     * @param fqn 关系 FQN
     * @return 重新生效后的关系实例 DTO
     */
    RelationInstanceDto reactivate(String fqn);

    /**
     * 校验下线前置条件（不执行实际下线）。
     *
     * @param fqn 生效关系 FQN
     * @return 下线前置条件校验结果
     */
    DeactivationCheckResult checkDeactivationPreconditions(String fqn);
}
