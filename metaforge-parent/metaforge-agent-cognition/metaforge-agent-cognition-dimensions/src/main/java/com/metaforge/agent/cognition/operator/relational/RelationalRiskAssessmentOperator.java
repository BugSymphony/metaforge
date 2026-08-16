package com.metaforge.agent.cognition.operator.relational;

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
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 变更风险评级——综合影响规模（正向扩散）、依赖强度（反向溯源）与约束冲突，
 * 输出风险分值与等级（LOW/MEDIUM/HIGH）及处理建议。
 */
@Component
public class RelationalRiskAssessmentOperator extends AbstractCognitionOperator {

    private static final double DEFAULT_IMPACT_WEIGHT = 0.5;
    private static final double DEFAULT_DEPENDENCY_WEIGHT = 0.3;
    private static final double DEFAULT_CONSTRAINT_WEIGHT = 0.2;

    @Override
    public String operatorId() {
        return "relational.risk-assessment";
    }

    @Override
    public DimensionCategory category() {
        return DimensionCategory.RELATIONAL;
    }

    @Override
    public CognitionResult execute(CognitionQueryContext context) {
        String entityFqn = context.entityFqn();
        if (entityFqn == null || entityFqn.isBlank()) {
            return wrapFailure("缺少 entityFqn 参数");
        }

        int maxDepth = resolveMaxDepth(context);
        Set<AssociationType> relationTypes = resolveRelationTypes(context);

        int forwardCount = 0;
        Object forwardResult = executeWithPort(() -> computeEngineReadPort.diffuseForward(
                new ImpactDiffusionRequest(entityFqn, TraversalDirection.FORWARD, maxDepth, relationTypes)));
        if (forwardResult instanceof CognitionResult cr) return cr;
        if (forwardResult instanceof ImpactTraceResult f) {
            forwardCount = f.totalImpacted();
        }

        int backwardCount = 0;
        int directDependents = 0;
        Object backwardResult = executeWithPort(() -> computeEngineReadPort.traceBackward(
                new ImpactDiffusionRequest(entityFqn, TraversalDirection.BACKWARD, maxDepth, relationTypes)));
        if (backwardResult instanceof CognitionResult cr) return cr;
        if (backwardResult instanceof ImpactTraceResult b) {
            backwardCount = b.totalImpacted();
            if (b.entities() != null) {
                for (ImpactTraceResult.ImpactEntityDetail e : b.entities()) {
                    if (e.depth() == 1) {
                        directDependents++;
                    }
                }
            }
        }

        int conflicts = resolveConstraintConflicts(entityFqn, forwardResult, maxDepth, relationTypes);

        Map<String, Object> factors = new LinkedHashMap<>();
        factors.put("impact_scope", buildFactor(forwardCount, 10, DEFAULT_IMPACT_WEIGHT));
        factors.put("dependency_strength", buildDependencyFactor(backwardCount, directDependents, DEFAULT_DEPENDENCY_WEIGHT));
        factors.put("constraint_conflicts", buildFactor(conflicts, 3, DEFAULT_CONSTRAINT_WEIGHT));

        double riskScore = scoreOf(factors.get("impact_scope")) * DEFAULT_IMPACT_WEIGHT
                + scoreOf(factors.get("dependency_strength")) * DEFAULT_DEPENDENCY_WEIGHT
                + scoreOf(factors.get("constraint_conflicts")) * DEFAULT_CONSTRAINT_WEIGHT;

        String riskLevel = riskScore >= 70 ? "HIGH" : (riskScore >= 40 ? "MEDIUM" : "LOW");
        String recommendation = "HIGH".equals(riskLevel) ? "需人工审批"
                : ("MEDIUM".equals(riskLevel) ? "需复核" : "可直接执行");

        Map<String, Object> resultData = new LinkedHashMap<>();
        resultData.put("entityFqn", entityFqn);
        resultData.put("risk_level", riskLevel);
        resultData.put("risk_score", Math.round(riskScore * 10.0) / 10.0);
        resultData.put("factors", factors);
        resultData.put("recommendation", recommendation);
        resultData.put("maxDepth", maxDepth);

        return CognitionResult.success(operatorId(), category(), resultData);
    }

    private Map<String, Object> buildFactor(int count, int saturation, double weight) {
        Map<String, Object> factor = new LinkedHashMap<>();
        factor.put("count", count);
        factor.put("weight", weight);
        factor.put("score", Math.round(min(count, saturation) * 100.0 / saturation));
        return factor;
    }

    private Map<String, Object> buildDependencyFactor(int total, int direct, double weight) {
        Map<String, Object> factor = new LinkedHashMap<>();
        factor.put("total", total);
        factor.put("direct_dependents", direct);
        factor.put("transitive", Math.max(total - direct, 0));
        factor.put("weight", weight);
        factor.put("score", Math.round(min(total, 10) * 100.0 / 10));
        return factor;
    }

    @SuppressWarnings("unchecked")
    private double scoreOf(Object factor) {
        if (factor instanceof Map<?, ?> m && m.get("score") instanceof Number n) {
            return n.doubleValue();
        }
        return 0;
    }

    private int resolveConstraintConflicts(String entityFqn, Object forwardResult,
                                           int maxDepth, Set<AssociationType> relationTypes) {
        Set<String> scopeFqns = new LinkedHashSet<>();
        scopeFqns.add(entityFqn);
        if (forwardResult instanceof ImpactTraceResult f && f.entities() != null) {
            for (ImpactTraceResult.ImpactEntityDetail e : f.entities()) {
                if (e.fqn() != null) {
                    scopeFqns.add(e.fqn());
                }
            }
        }
        Set<String> ruleFqns = new LinkedHashSet<>();
        for (String fqn : scopeFqns) {
            Object stepRule = executeWithPort(() -> queryRelationsBySchema(
                    fqn, RelationDirection.INBOUND, new LinkedHashMap<>(),
                    List.of(MetaforgeLibraryFqns.Relation.RULE_APPLIES_TO), null));
            if (stepRule instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof RelationInstanceDto dto && dto.getSourceEntityFqn() != null) {
                        ruleFqns.add(dto.getSourceEntityFqn());
                    }
                }
            }
            Object taskRule = executeWithPort(() -> queryRelationsBySchema(
                    fqn, RelationDirection.INBOUND, new LinkedHashMap<>(),
                    List.of(MetaforgeLibraryFqns.Relation.RULE_APPLIES_TO_TASK), null));
            if (taskRule instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof RelationInstanceDto dto && dto.getSourceEntityFqn() != null) {
                        ruleFqns.add(dto.getSourceEntityFqn());
                    }
                }
            }
        }
        return ruleFqns.size();
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

    @SuppressWarnings("unchecked")
    private Set<AssociationType> resolveRelationTypes(CognitionQueryContext context) {
        Map<String, Object> params = context.templateParams();
        if (params != null && params.get("relationTypes") instanceof List<?> types) {
            Set<AssociationType> set = new LinkedHashSet<>();
            for (Object t : types) {
                if (t instanceof AssociationType at) {
                    set.add(at);
                }
            }
            return set;
        }
        return Set.of();
    }

    private int min(int a, int b) {
        return Math.min(a, b);
    }
}
