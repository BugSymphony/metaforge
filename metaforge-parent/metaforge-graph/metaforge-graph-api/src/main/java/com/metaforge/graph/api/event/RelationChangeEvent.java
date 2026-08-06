package com.metaforge.graph.api.event;

import com.metaforge.graph.api.enums.ChangeType;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * 关系实例变更事件。
 * 在关系生效或下线事务成功提交后发布，供下游 BC 监听消费。
 */
public class RelationChangeEvent extends ApplicationEvent {

    private final String fqn;
    private final ChangeType changeType;
    private final Integer version;
    private final String relationSchemaFqn;
    private final String sourceEntityFqn;
    private final String targetEntityFqn;
    private final LocalDateTime eventTime;

    public RelationChangeEvent(Object source, String fqn, ChangeType changeType, Integer version,
                                String relationSchemaFqn, String sourceEntityFqn, String targetEntityFqn) {
        super(source);
        this.fqn = fqn;
        this.changeType = changeType;
        this.version = version;
        this.relationSchemaFqn = relationSchemaFqn;
        this.sourceEntityFqn = sourceEntityFqn;
        this.targetEntityFqn = targetEntityFqn;
        this.eventTime = LocalDateTime.now();
    }

    public String getFqn() { return fqn; }
    public ChangeType getChangeType() { return changeType; }
    public Integer getVersion() { return version; }
    public String getRelationSchemaFqn() { return relationSchemaFqn; }
    public String getSourceEntityFqn() { return sourceEntityFqn; }
    public String getTargetEntityFqn() { return targetEntityFqn; }
    public LocalDateTime getEventTime() { return eventTime; }
}
