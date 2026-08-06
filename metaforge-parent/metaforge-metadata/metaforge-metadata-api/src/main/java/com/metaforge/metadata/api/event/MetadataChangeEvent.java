package com.metaforge.metadata.api.event;

import com.metaforge.metadata.api.enums.ChangeType;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * 元数据变更事件。在元数据生效或下线时发布，供下游 BC 监听。
 */
public class MetadataChangeEvent extends ApplicationEvent {

    private final String fqn;
    private final ChangeType changeType;
    private final Integer version;
    private final LocalDateTime eventTime;

    public MetadataChangeEvent(Object source, String fqn, ChangeType changeType, Integer version) {
        super(source);
        this.fqn = fqn;
        this.changeType = changeType;
        this.version = version;
        this.eventTime = LocalDateTime.now();
    }

    public String getFqn() { return fqn; }
    public ChangeType getChangeType() { return changeType; }
    public Integer getVersion() { return version; }
    public LocalDateTime getEventTime() { return eventTime; }
}
