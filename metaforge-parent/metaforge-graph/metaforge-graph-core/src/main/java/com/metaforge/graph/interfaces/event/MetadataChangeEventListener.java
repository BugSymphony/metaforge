package com.metaforge.graph.interfaces.event;

import com.metaforge.graph.application.service.RelationAutoBuildService;
import com.metaforge.graph.infrastructure.event.IdempotencyStore;
import com.metaforge.metadata.api.enums.ChangeType;
import com.metaforge.metadata.api.event.MetadataChangeEvent;
import com.metaforge.metadata.api.event.MetadataChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 元数据变更事件监听器实现。
 *
 * <p>监听 metadata-management BC 的元数据变更事件，自动驱动关系实例的构建与销毁：
 * <ul>
 *   <li>ACTIVATE：解析实体内容中的关联引用字段，生成关系实例并自动生效</li>
 *   <li>DEPRECATE：查询该实体作为源端/目标端的所有生效关系，执行下线校验流程</li>
 * </ul>
 * 以实体 FQN + 版本号作为幂等键去重。
 */
@Component
public class MetadataChangeEventListener implements MetadataChangeListener {

    private static final Logger log = LoggerFactory.getLogger(MetadataChangeEventListener.class);

    private final RelationAutoBuildService autoBuildService;
    private final IdempotencyStore idempotencyStore;

    public MetadataChangeEventListener(RelationAutoBuildService autoBuildService,
                                        IdempotencyStore idempotencyStore) {
        this.autoBuildService = autoBuildService;
        this.idempotencyStore = idempotencyStore;
    }

    @Override
    public void onMetadataChange(MetadataChangeEvent event) {
        String fqn = event.getFqn();
        ChangeType changeType = event.getChangeType();
        Integer version = event.getVersion();

        log.info("收到元数据变更事件: fqn={}, changeType={}, version={}", fqn, changeType, version);

        if (idempotencyStore.isProcessed(fqn, version)) {
            log.debug("事件已处理，幂等跳过: fqn={}, version={}", fqn, version);
            return;
        }

        try {
            dispatchByChangeType(fqn, changeType, version);
            idempotencyStore.markProcessed(fqn, version);
            log.info("事件处理完成: fqn={}, changeType={}", fqn, changeType);
        } catch (Exception e) {
            log.error("事件处理异常: fqn={}, changeType={}", fqn, changeType, e);
        }
    }

    private void dispatchByChangeType(String fqn, ChangeType changeType, Integer version) {
        switch (changeType) {
            case ACTIVATE:
                handleActivate(fqn, version);
                break;
            case DEPRECATE:
                handleDeprecate(fqn);
                break;
            default:
                log.warn("未知变更类型: {}", changeType);
        }
    }

    private void handleActivate(String entityFqn, Integer version) {
        log.info("实体生效——触发关系自动构建: entity={}, version={}", entityFqn, version);

        // MVP 阶段：仅记录事件，自动构建由上游调用方通过 RelationAutoBuildService 显式触发
        // 实际构建逻辑需解析实体 content 中的关联引用字段，这里由调用方传入构建参数

        log.info("实体生效事件已接收: entity={}, version={}, 等待上游调用方触发自动构建", entityFqn, version);
    }

    private void handleDeprecate(String entityFqn) {
        log.info("实体下线——触发关联关系下线: entity={}", entityFqn);
        autoBuildService.deprecateRelationsOnEntityDeprecate(entityFqn);
    }
}
