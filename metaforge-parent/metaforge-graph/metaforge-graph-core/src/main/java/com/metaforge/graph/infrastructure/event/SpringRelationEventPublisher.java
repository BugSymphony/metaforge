package com.metaforge.graph.infrastructure.event;

import com.metaforge.graph.api.event.RelationChangeEvent;
import com.metaforge.graph.api.event.RelationChangeListener;
import com.metaforge.graph.domain.event.RelationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Set;

/**
 * Spring 事件发布器实现。
 * 使用 ApplicationEventPublisher + @TransactionalEventListener 确保事务成功后发布。
 */
@Component
public class SpringRelationEventPublisher implements RelationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SpringRelationEventPublisher.class);

    private final ApplicationEventPublisher eventPublisher;
    private final Set<RelationChangeListener> listeners;

    public SpringRelationEventPublisher(ApplicationEventPublisher eventPublisher,
                                         Set<RelationChangeListener> listeners) {
        this.eventPublisher = eventPublisher;
        this.listeners = listeners;
    }

    @Override
    public void publishActivated(RelationChangeEvent event) {
        log.info("发布关系生效事件: fqn={}, version={}", event.getFqn(), event.getVersion());
        publish(event);
    }

    @Override
    public void publishDeprecated(RelationChangeEvent event) {
        log.info("发布关系下线事件: fqn={}", event.getFqn());
        publish(event);
    }

    private void publish(RelationChangeEvent event) {
        eventPublisher.publishEvent(event);

        if (listeners.isEmpty()) {
            return;
        }

        for (RelationChangeListener listener : listeners) {
            try {
                listener.onRelationChange(event);
            } catch (Exception e) {
                log.warn("事件监听器处理异常, 不影响主事务: listener={}, error={}",
                        listener.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    /**
     * 事务后回调——确保在主事务成功提交后再发布事件。
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAfterCommit(RelationChangeEvent event) {
        log.debug("事务已提交, 事件已发布: fqn={}", event.getFqn());
    }
}
