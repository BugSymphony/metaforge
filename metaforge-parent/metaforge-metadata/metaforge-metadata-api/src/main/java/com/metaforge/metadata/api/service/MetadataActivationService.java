package com.metaforge.metadata.api.service;

import com.metaforge.metadata.api.annotation.OpenHostService;
import com.metaforge.metadata.api.dto.response.DeactivationCheckResult;
import com.metaforge.metadata.api.dto.response.MetadataEntityDto;

/**
 * 元数据版本生效与生命周期管控服务。
 * <p>
 * 生效操作为原子事务：同一事务内完成（1）主表写入/覆盖唯一生效版本、
 * （2）全量快照归档至历史表（版本号递增）、（3）删除草稿表对应记录。
 * 任意一步失败全量回滚不产生脏数据。
 */
@OpenHostService
public interface MetadataActivationService {

    /**
     * 对校验通过的草稿执行生效操作。
     * <p>
     * 生效前执行全量预校验：
     * <ul>
     *   <li>JSON Schema 结构合规校验</li>
     *   <li>组合层级合法性（父实体状态为 ACTIVE）</li>
     *   <li>元模型版本有效性校验</li>
     * </ul>
     * 原子事务步骤：
     * <ul>
     *   <li>主表写入/覆盖（版本号递增）</li>
     *   <li>历史表插入全量快照</li>
     *   <li>草稿表删除对应记录</li>
     * </ul>
     * 事务成功提交后发布变更事件（操作类型 = "生效"）。
     *
     * @param fqn 草稿 FQN
     * @return 生效后的元数据 DTO
     * @throws DraftNotFoundException      如果草稿不存在
     * @throws MetadataValidationException 如果预校验失败
     * @throws ActivationFailedException   如果生效事务失败
     */
    MetadataEntityDto activate(String fqn);

    /**
     * 对生效版本执行下线操作。
     * <p>
     * 下线前校验：
     * <ul>
     *   <li>外部活跃引用检查（调用语义关系网络模块）</li>
     *   <li>生效子实体状态检查（FQN 前缀匹配）</li>
     * </ul>
     * 存在任一拦截条件则拒绝下线并返回详细清单。
     * 原子事务：
     * <ul>
     *   <li>主表删除记录</li>
     *   <li>历史表原样保留归档（不做任何修改）</li>
     * </ul>
     * 事务成功提交后发布变更事件（操作类型 = "下线"）。
     *
     * @param fqn 生效元数据 FQN
     * @throws EntityNotFoundException    如果生效版本不存在
     * @throws DeactivationBlockedException 如果存在活跃引用或生效子实体
     */
    void deactivate(String fqn);

    /**
     * 基于历史归档版本重新生效。
     * <p>
     * 无修改时：直接从历史表恢复最新归档版本到主表，不新增历史记录。
     * 需修改时：调用 {@link MetadataDraftService#createDraftFromActive} 创建草稿后走标准生效流程。
     *
     * @param fqn 元数据 FQN
     * @return 重新生效后的元数据 DTO
     * @throws EntityNotFoundException 如果历史表中无归档记录
     */
    MetadataEntityDto reactivate(String fqn);

    /**
     * 校验下线前置条件。
     * <p>
     * 检查外部活跃引用与生效子实体状态，返回拦截清单。
     * 不执行实际下线操作。
     *
     * @param fqn 生效元数据 FQN
     * @return 下线前置条件校验结果
     */
    DeactivationCheckResult checkDeactivationPreconditions(String fqn);
}
