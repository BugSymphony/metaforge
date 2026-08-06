package com.metaforge.metadata.api.event;

/**
 * 元数据变更事件监听器接口，供下游 BC 实现。
 */
@FunctionalInterface
public interface MetadataChangeListener {
    void onMetadataChange(MetadataChangeEvent event);
}
