package com.metaforge.graph.domain.event;

import com.metaforge.graph.api.event.RelationChangeEvent;

/**
 * 领域事件发布器端口接口。
 * 定义生效/下线后发布事件的领域能力。
 */
public interface RelationEventPublisher {

    /**
     * 发布关系生效事件。
     */
    void publishActivated(RelationChangeEvent event);

    /**
     * 发布关系下线事件。
     */
    void publishDeprecated(RelationChangeEvent event);
}
