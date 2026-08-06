package com.metaforge.graph.api.event;

/**
 * 关系变更事件监听器接口，供下游 BC 实现。
 * 下游 BC 通过 Maven 依赖 metaforge-graph-api 模块后实现此接口消费事件。
 */
@FunctionalInterface
public interface RelationChangeListener {
    void onRelationChange(RelationChangeEvent event);
}
