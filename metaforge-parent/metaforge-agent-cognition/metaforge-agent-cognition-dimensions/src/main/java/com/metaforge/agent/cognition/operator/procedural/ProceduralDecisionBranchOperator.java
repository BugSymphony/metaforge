package com.metaforge.agent.cognition.operator.procedural;

import com.metaforge.agent.cognition.api.enums.DimensionCategory;
import com.metaforge.agent.cognition.api.spi.CognitionQueryContext;
import com.metaforge.agent.cognition.api.spi.CognitionResult;
import com.metaforge.agent.cognition.operator.common.AbstractCognitionOperator;
import com.metaforge.agent.cognition.operator.common.MetaforgeLibraryFqns;
import com.metaforge.computeengine.api.dto.request.ImpactDiffusionRequest;
import com.metaforge.computeengine.api.enums.TraversalDirection;
import com.metaforge.graph.api.dto.RelationInstanceDto;
import com.metaforge.metadata.api.dto.response.MetadataEntityDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 决策分支——按目标执行单元类型自适应解析决策走向。
 * <ul>
 *   <li>{@code DecisionStep} 本体：输出决策内容（条件/推荐/依据）+ 后继分支（{@code DecisionStepHasNext*}）</li>
 *   <li>{@code ExecutionStep}：简单分支（{@code StepHasNextStep} 出边 &gt; 1）+ 下游决策步骤（{@code StepHasNextDecisionStep}）</li>
 *   <li>{@code Task}：起点决策步骤（{@code TaskHasEntryDecisionStep}）及其分支</li>
 * </ul>
 */
@Component
public class ProceduralDecisionBranchOperator extends AbstractCognitionOperator {

    private static final List<String> DEFAULT_SEQUENCE_RELATIONS = List.of(
            MetaforgeLibraryFqns.Relation.STEP_HAS_NEXT_STEP);
    private static final List<String> DEFAULT_TASK_STEP_RELATIONS = List.of(
            MetaforgeLibraryFqns.Relation.TASK_HAS_ENTRY_STEP);
    private static final List<String> DEFAULT_TASK_DECISION_STEP_RELATIONS = List.of(
            MetaforgeLibraryFqns.Relation.TASK_HAS_ENTRY_DECISION_STEP);
    private static final List<String> DEFAULT_STEP_TO_DECISION_RELATIONS = List.of(
            MetaforgeLibraryFqns.Relation.STEP_HAS_NEXT_DECISION_STEP);
    private static final List<String> DEFAULT_DECISION_SUCCESSOR_RELATIONS = List.of(
            MetaforgeLibraryFqns.Relation.DECISION_STEP_HAS_NEXT_STEP,
            MetaforgeLibraryFqns.Relation.DECISION_STEP_HAS_NEXT_DECISION_STEP,
            MetaforgeLibraryFqns.Relation.DECISION_STEP_HAS_NEXT_TASK);

    @Override
    public String operatorId() {
        return "procedural.decision-branch";
    }

    @Override
    public DimensionCategory category() {
        return DimensionCategory.PROCEDURAL;
    }

    @Override
    public CognitionResult execute(CognitionQueryContext context) {
        String entityFqn = context.entityFqn();
        if (entityFqn == null || entityFqn.isBlank()) {
            return wrapFailure("缺少 entityFqn 参数");
        }

        Map<String, Object> config = context.operatorConfig();
        String targetType = resolveEntityType(entityFqn);

        if (MetaforgeLibraryFqns.Entity.DECISION_STEP.equals(targetType)) {
            return executeOnDecisionStep(entityFqn, config);
        }
        return executeOnStepOrTask(entityFqn, config);
    }

    /**
     * 目标为决策步骤本体：输出自身决策内容 + 后继分支 + 所属任务上下文。
     * current 承载当前决策详情，decisionSteps 仅列任务下其他决策步骤（避免与 current 重复）。
     */
    private CognitionResult executeOnDecisionStep(String entityFqn, Map<String, Object> config) {
        Map<String, Object> current = buildDecisionDetail(entityFqn, config);
        List<Map<String, Object>> otherDecisionSteps = new ArrayList<>();

        String taskFqn = resolveTaskFqn(entityFqn, config);
        if (taskFqn != null) {
            for (Map<String, Object> other : resolveDecisionSteps(taskFqn, config)) {
                if (!entityFqn.equals(other.get("fqn"))) {
                    otherDecisionSteps.add(other);
                }
            }
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> successors = (List<Map<String, Object>>) current.get("successors");

        Map<String, Object> resultData = new LinkedHashMap<>();
        resultData.put("targetType", MetaforgeLibraryFqns.Entity.DECISION_STEP);
        resultData.put("isDecisionBranch", successors != null && successors.size() > 1);
        resultData.put("branchCount", successors != null ? successors.size() : 0);
        resultData.put("current", current);
        resultData.put("decisionSteps", otherDecisionSteps);
        resultData.put("taskFqn", taskFqn);
        resultData.put("entityFqn", entityFqn);

        return CognitionResult.success(operatorId(), category(), resultData);
    }

    /**
     * 目标为执行步骤或任务：简单分支 + 下游决策步骤 + 任务级决策上下文。
     */
    private CognitionResult executeOnStepOrTask(String entityFqn, Map<String, Object> config) {
        Object outboundResult = executeWithPort(() -> queryRelationsBySchema(
                entityFqn, RelationDirection.OUTBOUND, config, DEFAULT_SEQUENCE_RELATIONS, null));
        if (outboundResult instanceof CognitionResult cr) return cr;

        List<RelationInstanceDto> outbound = extractRelations(outboundResult);
        boolean isDecisionBranch = outbound.size() > 1;

        List<Map<String, Object>> branches = new ArrayList<>();
        if (isDecisionBranch) {
            for (int i = 0; i < outbound.size(); i++) {
                RelationInstanceDto edge = outbound.get(i);
                String targetFqn = edge.getTargetEntityFqn();
                Object condition = edge.getContent() != null
                        ? edge.getContent().getOrDefault("condition", "无") : "无";

                Object downstream = fetchDownstreamImpact(targetFqn);
                String tendency = i == 0 ? "PRIMARY" : "ALTERNATIVE";

                Map<String, Object> branch = new LinkedHashMap<>();
                branch.put("targetFqn", targetFqn);
                branch.put("condition", condition);
                branch.put("tendency", tendency);
                branch.put("downstream_impact", downstream);
                branches.add(branch);
            }
        }

        Map<String, Object> resultData = new LinkedHashMap<>();
        resultData.put("targetType", resolveEntityType(entityFqn));
        resultData.put("isDecisionBranch", isDecisionBranch);
        resultData.put("branchCount", outbound.size());
        resultData.put("branches", branches);
        resultData.put("downstreamDecision", resolveDownstreamDecision(entityFqn, config));
        resultData.put("decisionSteps", resolveDecisionSteps(entityFqn, config));
        resultData.put("entityFqn", entityFqn);

        return CognitionResult.success(operatorId(), category(), resultData);
    }

    /**
     * 执行步骤的下游决策步骤：StepHasNextDecisionStep 出边。
     */
    private List<Map<String, Object>> resolveDownstreamDecision(String entityFqn, Map<String, Object> config) {
        List<Map<String, Object>> downstream = new ArrayList<>();
        Object relResult = executeWithPort(() -> queryRelationsBySchema(
                entityFqn, RelationDirection.OUTBOUND, config, DEFAULT_STEP_TO_DECISION_RELATIONS, null));
        if (!(relResult instanceof List<?> list)) {
            return downstream;
        }
        for (Object item : list) {
            if (!(item instanceof RelationInstanceDto dto) || dto.getTargetEntityFqn() == null) {
                continue;
            }
            Map<String, Object> decision = buildDecisionDetail(dto.getTargetEntityFqn(), config);
            decision.put("viaRelationFqn", dto.getRelationSchemaFqn());
            downstream.add(decision);
        }
        return downstream;
    }

    /**
     * 构建决策步骤详情：content 决策字段 + 后继分支（含 condition）。
     */
    private Map<String, Object> buildDecisionDetail(String decisionStepFqn, Map<String, Object> config) {
        Map<String, Object> decision = new LinkedHashMap<>();
        decision.put("fqn", decisionStepFqn);
        Object detail = executeWithPort(() -> metadataReadPort.getByFqn(decisionStepFqn));
        if (detail instanceof MetadataEntityDto dsDto) {
            decision.putAll(toContentMap(dsDto));
            decision.put("name", dsDto.getName());
            decision.put("description", dsDto.getDescription());
            decision.put("entitySchemaFqn", dsDto.getEntitySchemaFqn());
        }
        decision.put("successors", resolveSuccessors(decisionStepFqn, config));
        return decision;
    }

    /**
     * 决策步骤的后继分支：DecisionStepHasNextStep/DecisionStepHasNextDecisionStep/DecisionStepHasNextTask。
     */
    private List<Map<String, Object>> resolveSuccessors(String decisionStepFqn, Map<String, Object> config) {
        List<Map<String, Object>> successors = new ArrayList<>();
        Object relResult = executeWithPort(() -> queryRelationsBySchema(
                decisionStepFqn, RelationDirection.OUTBOUND, config, DEFAULT_DECISION_SUCCESSOR_RELATIONS, null));
        if (!(relResult instanceof List<?> list)) {
            return successors;
        }
        for (Object item : list) {
            if (!(item instanceof RelationInstanceDto dto) || dto.getTargetEntityFqn() == null) {
                continue;
            }
            Map<String, Object> successor = new LinkedHashMap<>();
            successor.put("targetFqn", dto.getTargetEntityFqn());
            successor.put("relationSchemaFqn", dto.getRelationSchemaFqn());
            successor.put("relationType", dto.getRelationType());
            Object condition = dto.getContent() != null
                    ? dto.getContent().getOrDefault("condition", "无") : "无";
            successor.put("condition", condition);
            successors.add(successor);
        }
        return successors;
    }

    /**
     * 任务下的决策步骤及其后继：TaskHasEntryDecisionStep 出边。
     */
    private List<Map<String, Object>> resolveDecisionSteps(String entityFqn, Map<String, Object> config) {
        List<Map<String, Object>> decisionSteps = new ArrayList<>();
        String taskFqn = resolveTaskFqn(entityFqn, config);
        if (taskFqn == null) {
            return decisionSteps;
        }

        Object relResult = executeWithPort(() -> queryRelationsBySchema(
                taskFqn, RelationDirection.OUTBOUND, config, DEFAULT_TASK_DECISION_STEP_RELATIONS, null));
        if (!(relResult instanceof List<?> list)) {
            return decisionSteps;
        }
        for (Object item : list) {
            if (item instanceof RelationInstanceDto dto && dto.getTargetEntityFqn() != null) {
                decisionSteps.add(buildDecisionDetail(dto.getTargetEntityFqn(), config));
            }
        }
        return decisionSteps;
    }

    /**
     * 定位任务 FQN：Task 直接返回；ExecutionStep 查 TaskHasEntryStep 入边；
     * DecisionStep 查 TaskHasEntryDecisionStep 入边。
     */
    private String resolveTaskFqn(String entityFqn, Map<String, Object> config) {
        Object entity = executeWithPort(() -> metadataReadPort.getByFqn(entityFqn));
        if (entity instanceof MetadataEntityDto dto
                && MetaforgeLibraryFqns.Entity.TASK.equals(dto.getEntitySchemaFqn())) {
            return entityFqn;
        }
        Object relResult = executeWithPort(() -> queryRelationsBySchema(
                entityFqn, RelationDirection.INBOUND, config, DEFAULT_TASK_STEP_RELATIONS, null));
        if (relResult instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof RelationInstanceDto dto && dto.getSourceEntityFqn() != null) {
                    return dto.getSourceEntityFqn();
                }
            }
        }
        Object decisionRelResult = executeWithPort(() -> queryRelationsBySchema(
                entityFqn, RelationDirection.INBOUND, config, DEFAULT_TASK_DECISION_STEP_RELATIONS, null));
        if (decisionRelResult instanceof List<?> decisionList) {
            for (Object item : decisionList) {
                if (item instanceof RelationInstanceDto dto && dto.getSourceEntityFqn() != null) {
                    return dto.getSourceEntityFqn();
                }
            }
        }
        return null;
    }

    /**
     * 解析实体类型（entitySchemaFqn）。
     */
    private String resolveEntityType(String entityFqn) {
        Object entity = executeWithPort(() -> metadataReadPort.getByFqn(entityFqn));
        if (entity instanceof MetadataEntityDto dto) {
            return dto.getEntitySchemaFqn();
        }
        return null;
    }

    private Object fetchDownstreamImpact(String targetFqn) {
        if (targetFqn == null) return Map.of();
        ImpactDiffusionRequest request = new ImpactDiffusionRequest(
                targetFqn, TraversalDirection.FORWARD, 3, Set.of());
        Object result = executeWithPort(() -> computeEngineReadPort.diffuseForward(request));
        if (result instanceof CognitionResult) return Map.of();
        return result;
    }

    private List<RelationInstanceDto> extractRelations(Object portResult) {
        List<RelationInstanceDto> result = new ArrayList<>();
        if (portResult instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof RelationInstanceDto dto) {
                    result.add(dto);
                }
            }
        }
        return result;
    }
}
