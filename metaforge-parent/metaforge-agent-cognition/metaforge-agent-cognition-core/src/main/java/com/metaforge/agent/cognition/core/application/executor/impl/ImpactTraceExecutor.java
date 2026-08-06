package com.metaforge.agent.cognition.core.application.executor.impl;

import com.metaforge.agent.cognition.api.enums.PerspectiveCode;
import com.metaforge.agent.cognition.api.perspective.PerspectiveExecutionContext;
import com.metaforge.agent.cognition.api.perspective.PerspectiveExecutor;
import com.metaforge.agent.cognition.core.domain.model.entity.ImpactTrace;
import com.metaforge.computeengine.api.dto.response.ImpactTraceResult;
import com.metaforge.metadata.api.dto.response.MetadataEntityDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ImpactTraceExecutor implements PerspectiveExecutor {

    private static final Logger log = LoggerFactory.getLogger(ImpactTraceExecutor.class);

    private final ExecutorSupport support;

    public ImpactTraceExecutor(ExecutorSupport support) {
        this.support = support;
    }

    @Override
    public PerspectiveCode supportedPerspective() {
        return PerspectiveCode.IMPACT_TRACE;
    }

    @Override
    public Object execute(PerspectiveExecutionContext ctx) {
        log.debug("执行影响追溯视角(compute-engine): entityFqn={}", ctx.entityFqn());

        ImpactTrace trace = new ImpactTrace();
        trace.setSourceFqn(ctx.entityFqn());
        trace.setForwardImpact(new LinkedHashMap<>());
        trace.setBackwardDependency(new LinkedHashMap<>());
        trace.setImpactPaths(new ArrayList<>());

        if (ctx.entityFqn() == null) {
            return trace;
        }

        ImpactTraceResult forward = support.diffuseForward(ctx.entityFqn(), null, 3);
        if (forward != null && forward.layerStats() != null) {
            Map<Integer, List<ImpactTrace.ImpactEntity>> forwardMap = new LinkedHashMap<>();
            for (Map.Entry<Integer, List<ImpactTraceResult.ImpactEntityDetail>> entry : forward.layerStats().entrySet()) {
                List<ImpactTrace.ImpactEntity> entities = new ArrayList<>();
                for (ImpactTraceResult.ImpactEntityDetail detail : entry.getValue()) {
                    entities.add(buildImpactEntity(detail.fqn(), detail.depth()));
                }
                forwardMap.put(entry.getKey(), entities);
            }
            trace.setForwardImpact(forwardMap);
        }

        ImpactTraceResult backward = support.traceBackward(ctx.entityFqn(), null, 3);
        if (backward != null && backward.layerStats() != null) {
            Map<Integer, List<ImpactTrace.ImpactEntity>> backwardMap = new LinkedHashMap<>();
            for (Map.Entry<Integer, List<ImpactTraceResult.ImpactEntityDetail>> entry : backward.layerStats().entrySet()) {
                List<ImpactTrace.ImpactEntity> entities = new ArrayList<>();
                for (ImpactTraceResult.ImpactEntityDetail detail : entry.getValue()) {
                    entities.add(buildImpactEntity(detail.fqn(), detail.depth()));
                }
                backwardMap.put(entry.getKey(), entities);
            }
            trace.setBackwardDependency(backwardMap);
        }

        trace.setImpactPaths(buildImpactPaths(trace.getForwardImpact()));
        return trace;
    }

    private ImpactTrace.ImpactEntity buildImpactEntity(String fqn, int depth) {
        ImpactTrace.ImpactEntity entity = new ImpactTrace.ImpactEntity();
        entity.setFqn(fqn);
        entity.setName(resolveName(fqn));
        entity.setEntitySchemaFqn(resolveSchemaFqn(fqn));
        entity.setDepth(depth);
        return entity;
    }

    private List<ImpactTrace.ImpactPath> buildImpactPaths(Map<Integer, List<ImpactTrace.ImpactEntity>> forward) {
        List<ImpactTrace.ImpactPath> paths = new ArrayList<>();
        if (forward == null) {
            return paths;
        }
        for (Map.Entry<Integer, List<ImpactTrace.ImpactEntity>> entry : forward.entrySet()) {
            for (ImpactTrace.ImpactEntity entity : entry.getValue()) {
                ImpactTrace.ImpactPath path = new ImpactTrace.ImpactPath();
                path.setTargetFqn(entity.getFqn());
                path.setPathLength(entry.getKey());
                path.setHopEntities(List.of(entity.getFqn()));
                path.setHopRelations(new ArrayList<>());
                path.setSemanticDescription("影响链深度 " + entry.getKey());
                paths.add(path);
            }
        }
        return paths;
    }

    private String resolveName(String fqn) {
        if (fqn == null) return "";
        Object raw = support.metadata().getByFqn(fqn);
        if (raw instanceof MetadataEntityDto entity && entity.getName() != null) {
            return entity.getName();
        }
        String[] parts = fqn.split("\\.");
        return parts.length > 0 ? parts[parts.length - 1] : fqn;
    }

    private String resolveSchemaFqn(String fqn) {
        if (fqn == null) return null;
        Object raw = support.metadata().getByFqn(fqn);
        if (raw instanceof MetadataEntityDto entity) {
            return entity.getEntitySchemaFqn();
        }
        return null;
    }
}
