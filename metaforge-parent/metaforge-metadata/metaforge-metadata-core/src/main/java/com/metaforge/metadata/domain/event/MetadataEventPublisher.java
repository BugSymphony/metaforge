package com.metaforge.metadata.domain.event;

import com.metaforge.metadata.api.event.MetadataChangeEvent;

public interface MetadataEventPublisher {
    void publish(MetadataChangeEvent event);
}
