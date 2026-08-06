package com.metaforge.agent.cognition.core.domain.service;

import com.metaforge.agent.cognition.api.enums.ContextMode;
import com.metaforge.agent.cognition.core.domain.model.valueobject.AdjacentContext;
import com.metaforge.agent.cognition.core.domain.port.GraphClientPort;
import com.metaforge.agent.cognition.core.domain.port.ComputeEngineClientPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ScopeResolutionServiceImpl {

    private static final Logger log = LoggerFactory.getLogger(ScopeResolutionServiceImpl.class);

    private final GraphClientPort graphClientPort;
    private final ComputeEngineClientPort computeEngineClientPort;

    public ScopeResolutionServiceImpl(GraphClientPort graphClientPort,
                                       ComputeEngineClientPort computeEngineClientPort) {
        this.graphClientPort = graphClientPort;
        this.computeEngineClientPort = computeEngineClientPort;
    }

    /**
     * 为 ENTITY_LEVEL 上下文构建 adjacent_context。
     * 查询 PROCESS_SEQUENCE 出边获取后几步 FQN，入边获取前一步 FQN。
     */
    public AdjacentContext buildAdjacentContext(String entityFqn) {
        if (entityFqn == null || entityFqn.isBlank()) {
            return new AdjacentContext(List.of(), List.of(), List.of(), List.of());
        }

        List<String> previousSteps = new ArrayList<>();
        List<String> nextSteps = new ArrayList<>();
        List<String> upstreamEntities = new ArrayList<>();
        List<String> downstreamEntities = new ArrayList<>();

        try {
            Object inbound = graphClientPort.getInboundRelations(entityFqn,
                    List.of("PROCESS_SEQUENCE"), List.of());
            if (inbound != null) {
                previousSteps = extractFqns(inbound);
            }
        } catch (Exception e) {
            log.warn("查询 PROCESS_SEQUENCE 入边失败: {}", entityFqn, e);
        }

        try {
            Object outbound = graphClientPort.getOutboundRelations(entityFqn,
                    List.of("PROCESS_SEQUENCE"), List.of());
            if (outbound != null) {
                nextSteps = extractFqns(outbound);
            }
        } catch (Exception e) {
            log.warn("查询 PROCESS_SEQUENCE 出边失败: {}", entityFqn, e);
        }

        try {
            Object upstream = graphClientPort.getInboundRelations(entityFqn,
                    List.of("DEPENDENCY_INFLUENCE"), List.of());
            if (upstream != null) {
                upstreamEntities = extractFqns(upstream);
            }
        } catch (Exception e) {
            log.warn("查询 DEPENDENCY_INFLUENCE 入边失败: {}", entityFqn, e);
        }

        try {
            Object downstream = graphClientPort.getOutboundRelations(entityFqn,
                    List.of("DEPENDENCY_INFLUENCE"), List.of());
            if (downstream != null) {
                downstreamEntities = extractFqns(downstream);
            }
        } catch (Exception e) {
            log.warn("查询 DEPENDENCY_INFLUENCE 出边失败: {}", entityFqn, e);
        }

        return new AdjacentContext(previousSteps, nextSteps, upstreamEntities, downstreamEntities);
    }

    /**
     * 判断指定作用域的视角是否应在当前 contextMode 下执行。
     */
    public boolean isApplicableInContext(String perspectiveScope, ContextMode contextMode) {
        if (contextMode == ContextMode.BUNDLE_LEVEL) {
            return "BUNDLE".equals(perspectiveScope) || "BOTH".equals(perspectiveScope);
        } else {
            return "ENTITY".equals(perspectiveScope) || "BOTH".equals(perspectiveScope);
        }
    }

    private List<String> extractFqns(Object result) {
        List<String> fqns = new ArrayList<>();
        if (result instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof String s) {
                    fqns.add(s);
                }
            }
        }
        return fqns;
    }
}
