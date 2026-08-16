package com.metaforge.agent.cognition.operator.capability;

import com.metaforge.agent.cognition.api.enums.DimensionCategory;
import com.metaforge.agent.cognition.api.spi.CognitionQueryContext;
import com.metaforge.agent.cognition.api.spi.CognitionResult;
import com.metaforge.agent.cognition.operator.common.AbstractCognitionOperator;
import com.metaforge.agent.cognition.operator.common.MetaforgeLibraryFqns;
import com.metaforge.computeengine.api.dto.request.ImpactDiffusionRequest;
import com.metaforge.computeengine.api.dto.response.ImpactTraceResult;
import com.metaforge.computeengine.api.enums.TraversalDirection;
import com.metaforge.graph.api.dto.RelationInstanceDto;
import com.metaforge.metadata.api.dto.response.MetadataEntityDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 回归范围建议——根据变更正向影响范围，反查关联的执行单元（能力/步骤/任务/协议），
 * 形成回归验证清单。
 */
@Component
public class CapabilityRegressionScopeOperator extends AbstractCognitionOperator {

    @Override
    public String operatorId() {
        return "capability.regression-scope";
    }

    @Override
    public DimensionCategory category() {
        return DimensionCategory.CAPABILITY;
    }

    @Override
    public CognitionResult execute(CognitionQueryContext context) {
        String entityFqn = context.entityFqn();
        if (entityFqn == null || entityFqn.isBlank()) {
            return wrapFailure("缺少 entityFqn 参数");
        }

        int maxDepth = resolveMaxDepth(context);
        Object forwardResult = executeWithPort(() -> computeEngineReadPort.diffuseForward(
                new ImpactDiffusionRequest(entityFqn, TraversalDirection.FORWARD, maxDepth, Set.of())));
        if (forwardResult instanceof CognitionResult cr) return cr;

        Set<String> scopeFqns = new LinkedHashSet<>();
        scopeFqns.add(entityFqn);
        if (forwardResult instanceof ImpactTraceResult f && f.entities() != null) {
            for (ImpactTraceResult.ImpactEntityDetail e : f.entities()) {
                if (e.fqn() != null) {
                    scopeFqns.add(e.fqn());
                }
            }
        }

        Map<String, Map<String, Object>> capabilities = new LinkedHashMap<>();
        Map<String, Map<String, Object>> steps = new LinkedHashMap<>();
        Map<String, Map<String, Object>> tasks = new LinkedHashMap<>();

        for (String fqn : scopeFqns) {
            collectByType(fqn, capabilities, steps, tasks);
        }

        List<String> checklist = new ArrayList<>();
        capabilities.values().forEach(c -> checklist.add(c.get("name") + " 需回归"));
        steps.values().forEach(s -> checklist.add(s.get("name") + " 需回归"));
        tasks.values().forEach(t -> checklist.add(t.get("name") + " 需回归"));

        Map<String, Object> resultData = new LinkedHashMap<>();
        resultData.put("entityFqn", entityFqn);
        resultData.put("affected_capabilities", new ArrayList<>(capabilities.values()));
        resultData.put("affected_steps", new ArrayList<>(steps.values()));
        resultData.put("affected_tasks", new ArrayList<>(tasks.values()));
        resultData.put("regression_checklist", checklist);
        resultData.put("count", capabilities.size() + steps.size() + tasks.size());
        resultData.put("maxDepth", maxDepth);

        return CognitionResult.success(operatorId(), category(), resultData);
    }

    private void collectByType(String fqn,
                               Map<String, Map<String, Object>> capabilities,
                               Map<String, Map<String, Object>> steps,
                               Map<String, Map<String, Object>> tasks) {
        Object entityResult = executeWithPort(() -> metadataReadPort.getByFqn(fqn));
        if (!(entityResult instanceof MetadataEntityDto dto)) {
            return;
        }
        String schemaFqn = dto.getEntitySchemaFqn();

        if (MetaforgeLibraryFqns.Entity.CAPABILITY.equals(schemaFqn)) {
            Map<String, Object> cap = buildSummary(dto);
            Object protoResult = executeWithPort(() -> queryRelationsBySchema(
                    fqn, RelationDirection.OUTBOUND, new LinkedHashMap<>(),
                    Collections.emptyList(), MetaforgeLibraryFqns.PROTOCOL_RELATION_PREFIX));
            if (protoResult instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof RelationInstanceDto rdto && rdto.getTargetEntityFqn() != null) {
                        cap.put("protocolFqn", rdto.getTargetEntityFqn());
                        break;
                    }
                }
            }
            capabilities.putIfAbsent(fqn, cap);
            return;
        }

        if (MetaforgeLibraryFqns.Entity.EXECUTION_STEP.equals(schemaFqn)) {
            steps.putIfAbsent(fqn, buildSummary(dto));
            Object capResult = executeWithPort(() -> queryRelationsBySchema(
                    fqn, RelationDirection.OUTBOUND, new LinkedHashMap<>(),
                    List.of(MetaforgeLibraryFqns.Relation.STEP_USES_CAPABILITY), null));
            if (capResult instanceof List<?> list) {
                for (Object item : list) {
                    if (!(item instanceof RelationInstanceDto rdto) || rdto.getTargetEntityFqn() == null) {
                        continue;
                    }
                    String capFqn = rdto.getTargetEntityFqn();
                    Object capEntity = executeWithPort(() -> metadataReadPort.getByFqn(capFqn));
                    if (capEntity instanceof MetadataEntityDto capDto) {
                        capabilities.putIfAbsent(capFqn, buildSummary(capDto));
                    }
                }
            }
            return;
        }

        if (MetaforgeLibraryFqns.Entity.TASK.equals(schemaFqn)) {
            tasks.putIfAbsent(fqn, buildSummary(dto));
        }
    }

    private Map<String, Object> buildSummary(MetadataEntityDto dto) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("fqn", dto.getFqn());
        summary.put("name", dto.getName());
        summary.put("entitySchemaFqn", dto.getEntitySchemaFqn());
        return summary;
    }

    private int resolveMaxDepth(CognitionQueryContext context) {
        Map<String, Object> params = context.templateParams();
        if (params != null) {
            for (String key : new String[]{"max_depth", "maxDepth"}) {
                if (params.containsKey(key)) {
                    try {
                        return Integer.parseInt(params.get(key).toString());
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return 3;
    }
}
