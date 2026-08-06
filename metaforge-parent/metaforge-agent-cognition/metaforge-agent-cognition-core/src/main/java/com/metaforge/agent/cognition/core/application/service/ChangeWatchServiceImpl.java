package com.metaforge.agent.cognition.core.application.service;

import com.metaforge.agent.cognition.core.domain.model.entity.ImpactTrace;
import com.metaforge.agent.cognition.core.domain.port.ComputeEngineClientPort;
import com.metaforge.agent.cognition.core.domain.service.ChangeWatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChangeWatchServiceImpl implements ChangeWatchService {

    private static final Logger log = LoggerFactory.getLogger(ChangeWatchServiceImpl.class);

    private final ComputeEngineClientPort computeEngineClientPort;

    public ChangeWatchServiceImpl(ComputeEngineClientPort computeEngineClientPort) {
        this.computeEngineClientPort = computeEngineClientPort;
    }

    @Override
    public ImpactTrace handleMetadataChange(String changedEntityFqn) {
        log.info("处理元数据变更事件: changedEntityFqn={}", changedEntityFqn);

        ImpactTrace trace = new ImpactTrace();
        trace.setSourceFqn(changedEntityFqn);

        try {
            computeEngineClientPort.diffuseForward(changedEntityFqn,
                    List.of("DEPENDENCY_INFLUENCE"), 3);
            log.debug("影响扩散完成: affectedEntities={}", changedEntityFqn);
        } catch (Exception e) {
            log.warn("变更影响扩散查询失败: entityFqn={}, error={}", changedEntityFqn, e.getMessage());
        }

        return trace;
    }

    @Override
    public ImpactTrace handleRelationChange(String changedRelationFqn) {
        log.info("处理关系变更事件: changedRelationFqn={}", changedRelationFqn);

        ImpactTrace trace = new ImpactTrace();
        trace.setSourceFqn(changedRelationFqn);

        try {
            computeEngineClientPort.diffuseForward(changedRelationFqn,
                    List.of("DEPENDENCY_INFLUENCE", "ASSOCIATION_REFERENCE"), 3);
            log.debug("关系变更影响扩散完成: relationFqn={}", changedRelationFqn);
        } catch (Exception e) {
            log.warn("关系变更影响扩散查询失败: relationFqn={}, error={}", changedRelationFqn, e.getMessage());
        }

        return trace;
    }
}
