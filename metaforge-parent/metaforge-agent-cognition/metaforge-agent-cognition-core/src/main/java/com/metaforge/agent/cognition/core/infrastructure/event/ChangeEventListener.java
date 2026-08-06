package com.metaforge.agent.cognition.core.infrastructure.event;

import com.metaforge.agent.cognition.core.domain.service.ChangeWatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ChangeEventListener {

    private static final Logger log = LoggerFactory.getLogger(ChangeEventListener.class);

    private final ChangeWatchService changeWatchService;

    public ChangeEventListener(ChangeWatchService changeWatchService) {
        this.changeWatchService = changeWatchService;
    }

    @EventListener
    public void handleMetadataChange(Object event) {
        try {
            String changedEntityFqn = extractEntityFqn(event);
            if (changedEntityFqn != null) {
                changeWatchService.handleMetadataChange(changedEntityFqn);
            }
        } catch (Exception e) {
            log.warn("处理元数据变更事件失败: best-effort, skipping", e);
        }
    }

    @EventListener
    public void handleRelationChange(Object event) {
        try {
            String changedRelationFqn = extractRelationFqn(event);
            if (changedRelationFqn != null) {
                changeWatchService.handleRelationChange(changedRelationFqn);
            }
        } catch (Exception e) {
            log.warn("处理关系变更事件失败: best-effort, skipping", e);
        }
    }

    private String extractEntityFqn(Object event) {
        if (event == null) return null;
        try {
            var method = event.getClass().getMethod("getEntityFqn");
            return (String) method.invoke(event);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractRelationFqn(Object event) {
        if (event == null) return null;
        try {
            var method = event.getClass().getMethod("getRelationFqn");
            return (String) method.invoke(event);
        } catch (Exception e) {
            return null;
        }
    }
}
