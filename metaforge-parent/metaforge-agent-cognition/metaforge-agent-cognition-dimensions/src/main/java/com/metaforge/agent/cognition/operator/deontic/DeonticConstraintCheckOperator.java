package com.metaforge.agent.cognition.operator.deontic;

import com.metaforge.agent.cognition.api.enums.DimensionCategory;
import com.metaforge.agent.cognition.api.spi.CognitionQueryContext;
import com.metaforge.agent.cognition.api.spi.CognitionResult;
import com.metaforge.agent.cognition.operator.common.AbstractCognitionOperator;
import com.metaforge.agent.cognition.operator.common.MetaforgeLibraryFqns;
import com.metaforge.computeengine.api.dto.request.ImpactDiffusionRequest;
import com.metaforge.computeengine.api.dto.response.ImpactTraceResult;
import com.metaforge.computeengine.api.enums.AssociationType;
import com.metaforge.computeengine.api.enums.TraversalDirection;
import com.metaforge.graph.api.dto.RelationInstanceDto;
import com.metaforge.metadata.api.dto.response.MetadataEntityDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 规则冲突检测——评估变更（MODIFY/DELETE/CREATE）在影响范围内是否触及约束规则，
 * 按规则级别（MANDATORY 等）判定冲突与阻断。
 */
@Component
public class DeonticConstraintCheckOperator extends AbstractCognitionOperator {

    private static final String DEFAULT_CHANGE_TYPE = "MODIFY";

    @Override
    public String operatorId() {
        return "deontic.constraint-check";
    }

    @Override
    public DimensionCategory category() {
        return DimensionCategory.DEONTIC;
    }

    @Override
    public CognitionResult execute(CognitionQueryContext context) {
        String entityFqn = context.entityFqn();
        if (entityFqn == null || entityFqn.isBlank()) {
            return wrapFailure("缺少 entityFqn 参数");
        }

        int maxDepth = resolveMaxDepth(context);
        String changeType = resolveChangeType(context);

        Object forwardResult = executeWithPort(() -> computeEngineReadPort.diffuseForward(
                new ImpactDiffusionRequest(entityFqn, TraversalDirection.FORWARD, maxDepth, Set.of())));
        if (forwardResult instanceof CognitionResult cr) return cr;

        Set<String> scopeFqns = resolveScopeFqns(entityFqn, forwardResult);

        List<Map<String, Object>> conflicts = new ArrayList<>();
        for (String fqn : scopeFqns) {
            collectRuleConflicts(fqn, MetaforgeLibraryFqns.Relation.RULE_APPLIES_TO, conflicts);
            collectRuleConflicts(fqn, MetaforgeLibraryFqns.Relation.RULE_APPLIES_TO_TASK, conflicts);
        }

        for (Map<String, Object> conflict : conflicts) {
            enrichRule(conflict);
            conflict.put("impact", resolveImpact((String) conflict.get("constraintLevel"), changeType));
        }

        boolean blocking = conflicts.stream()
                .anyMatch(c -> "高冲突".equals(c.get("impact")));

        Map<String, Object> resultData = new LinkedHashMap<>();
        resultData.put("entityFqn", entityFqn);
        resultData.put("change_type", changeType);
        resultData.put("conflicts", conflicts);
        resultData.put("conflict_count", conflicts.size());
        resultData.put("blocking", blocking);

        return CognitionResult.success(operatorId(), category(), resultData);
    }

    private Set<String> resolveScopeFqns(String entityFqn, Object forwardResult) {
        Set<String> fqns = new LinkedHashSet<>();
        fqns.add(entityFqn);
        if (forwardResult instanceof ImpactTraceResult f && f.entities() != null) {
            for (ImpactTraceResult.ImpactEntityDetail e : f.entities()) {
                if (e.fqn() != null) {
                    fqns.add(e.fqn());
                }
            }
        }
        return fqns;
    }

    private void collectRuleConflicts(String appliedEntityFqn, String relationSchemaFqn,
                                      List<Map<String, Object>> conflicts) {
        Object relResult = executeWithPort(() -> queryRelationsBySchema(
                appliedEntityFqn, RelationDirection.INBOUND, new LinkedHashMap<>(),
                List.of(relationSchemaFqn), null));
        if (!(relResult instanceof List<?> list)) {
            return;
        }
        for (Object item : list) {
            if (!(item instanceof RelationInstanceDto dto) || dto.getSourceEntityFqn() == null) {
                continue;
            }
            Map<String, Object> conflict = new LinkedHashMap<>();
            conflict.put("ruleFqn", dto.getSourceEntityFqn());
            conflict.put("relationSchemaFqn", dto.getRelationSchemaFqn());
            conflict.put("appliedEntityFqn", appliedEntityFqn);
            conflicts.add(conflict);
        }
    }

    private void enrichRule(Map<String, Object> conflict) {
        Object ruleResult = executeWithPort(() -> metadataReadPort.getByFqn((String) conflict.get("ruleFqn")));
        if (ruleResult instanceof MetadataEntityDto dto) {
            conflict.put("ruleName", dto.getName());
            conflict.put("constraintLevel", resolveContentValue(dto, "constraint_level"));
            conflict.put("condition", resolveContentValue(dto, "condition"));
        } else {
            conflict.put("ruleName", conflict.get("ruleFqn"));
            conflict.put("constraintLevel", "UNKNOWN");
            conflict.put("condition", null);
        }
    }

    private String resolveImpact(String constraintLevel, String changeType) {
        if ("MANDATORY".equalsIgnoreCase(constraintLevel) && "DELETE".equalsIgnoreCase(changeType)) {
            return "高冲突";
        }
        if ("DELETE".equalsIgnoreCase(changeType)) {
            return "可能违反";
        }
        if ("MODIFY".equalsIgnoreCase(changeType)) {
            return "需检查";
        }
        return "低风险";
    }

    private String resolveChangeType(CognitionQueryContext context) {
        Map<String, Object> params = context.templateParams();
        if (params != null && params.get("change_type") instanceof String t && !t.isBlank()) {
            return t.trim().toUpperCase();
        }
        return DEFAULT_CHANGE_TYPE;
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
