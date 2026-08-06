package com.metaforge.agent.cognition.core.application.executor;

import com.metaforge.agent.cognition.api.enums.PerspectiveCode;
import com.metaforge.agent.cognition.api.perspective.PerspectiveExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PerspectiveRegistry {

    private static final Logger log = LoggerFactory.getLogger(PerspectiveRegistry.class);

    private final Map<String, PerspectiveExecutor> executorMap = new ConcurrentHashMap<>();

    public PerspectiveRegistry(List<PerspectiveExecutor> executors) {
        for (PerspectiveExecutor executor : executors) {
            PerspectiveCode code = executor.supportedPerspective();
            executorMap.put(code.getValue(), executor);
            log.info("注册视角执行器: {} -> {}", code.getValue(), executor.getClass().getSimpleName());
        }
        log.info("共注册 {} 个视角执行器", executorMap.size());
    }

    public PerspectiveExecutor getExecutor(String perspectiveId) {
        PerspectiveExecutor executor = executorMap.get(perspectiveId);
        if (executor == null) {
            log.warn("未找到视角执行器: {}", perspectiveId);
        }
        return executor;
    }

    public Map<String, PerspectiveExecutor> getAllExecutors() {
        return executorMap;
    }
}
