package com.metaforge.agent.cognition.core.domain.service;

import com.metaforge.agent.cognition.core.domain.port.ComputeEngineClientPort;
import com.metaforge.agent.cognition.core.domain.port.GraphClientPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ScopeNarrowingServiceImpl implements ScopeNarrowingService {

    private static final Logger log = LoggerFactory.getLogger(ScopeNarrowingServiceImpl.class);

    private final ComputeEngineClientPort computeEngineClientPort;
    private final GraphClientPort graphClientPort;

    public ScopeNarrowingServiceImpl(ComputeEngineClientPort computeEngineClientPort,
                                      GraphClientPort graphClientPort) {
        this.computeEngineClientPort = computeEngineClientPort;
        this.graphClientPort = graphClientPort;
    }

    @Override
    public NarrowedScope narrow(String entryEntityFqn) {
        log.info("执行三层作用域收窄: entryEntityFqn={}", entryEntityFqn);

        if (entryEntityFqn == null || entryEntityFqn.isBlank()) {
            log.warn("入口实体 FQN 为空，返回空收窄作用域");
            return new NarrowedScope(List.of(), List.of(), List.of());
        }

        List<String> blueprintStepFqns = narrowBlueprint(entryEntityFqn);
        Set<String> relatedEntityFqns = collectRelatedEntities(blueprintStepFqns);
        Set<String> relatedSchemaFqns = narrowSchemas(relatedEntityFqns);

        NarrowedScope scope = new NarrowedScope(
                new ArrayList<>(blueprintStepFqns),
                new ArrayList<>(relatedEntityFqns),
                new ArrayList<>(relatedSchemaFqns));

        log.info("作用域收窄完成: blueprintSteps={}, relatedEntities={}, relatedSchemas={}",
                scope.blueprintStepFqns().size(),
                scope.relatedEntityFqns().size(),
                scope.relatedSchemaFqns().size());

        return scope;
    }

    private List<String> narrowBlueprint(String entryEntityFqn) {
        try {
            Object adjacency = computeEngineClientPort.queryAdjacency(
                    entryEntityFqn, "BOTH", 1, List.of("PROCESS_SEQUENCE"));
            return extractFqns(adjacency);
        } catch (Exception e) {
            log.warn("蓝图收窄失败: entryEntityFqn={}", entryEntityFqn, e);
            return List.of(entryEntityFqn);
        }
    }

    private Set<String> collectRelatedEntities(List<String> stepFqns) {
        Set<String> related = new LinkedHashSet<>();
        for (String fqn : stepFqns) {
            try {
                Object outbound = graphClientPort.getOutboundRelations(fqn,
                        List.of("PROCESS_SEQUENCE", "DEPENDENCY_INFLUENCE"), List.of());
                related.addAll(extractFqns(outbound));

                Object inbound = graphClientPort.getInboundRelations(fqn,
                        List.of("PROCESS_SEQUENCE", "DEPENDENCY_INFLUENCE"), List.of());
                related.addAll(extractFqns(inbound));
            } catch (Exception e) {
                log.debug("实体收集失败: fqn={}", fqn, e);
            }
        }
        return related;
    }

    private Set<String> narrowSchemas(Set<String> entityFqns) {
        Set<String> schemas = new LinkedHashSet<>();
        for (String fqn : entityFqns) {
            String schemaFqn = extractSchemaFqn(fqn);
            if (schemaFqn != null) {
                schemas.add(schemaFqn);
            }
        }
        log.debug("Schema 收窄: 从 {} 个实体去重得 {} 个 Schema", entityFqns.size(), schemas.size());
        return schemas;
    }

    private String extractSchemaFqn(String entityFqn) {
        if (entityFqn == null) return null;
        int lastDot = entityFqn.lastIndexOf('.');
        if (lastDot > 0) {
            return entityFqn.substring(0, lastDot);
        }
        return entityFqn;
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
