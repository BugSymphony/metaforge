package com.metaforge.agent.cognition.operator.procedural;

import com.metaforge.agent.cognition.api.enums.DimensionCategory;
import com.metaforge.agent.cognition.api.spi.CognitionQueryContext;
import com.metaforge.agent.cognition.api.spi.CognitionResult;
import com.metaforge.agent.cognition.operator.common.AbstractCognitionOperator;
import com.metaforge.agent.cognition.operator.common.MetaforgeLibraryFqns;
import com.metaforge.graph.api.dto.RelationInstanceDto;
import com.metaforge.metadata.api.dto.response.MetadataEntityDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 流程蓝图——端到端步骤序列。支持 Task 锚点：先经 {@code entry_step_fqn} /
 * {@code TaskHasEntryStep} 解析入口步，再沿 {@code StepHasNextStep}（PROCESS_SEQUENCE）
 * 逐跳遍历构建完整步骤链（不依赖 compute-engine 的多跳推理 hop 上限）。
 */
@Component
public class ProceduralFlowBlueprintOperator extends AbstractCognitionOperator {

    private static final int DEFAULT_MAX_HOPS = 10;

    private static final List<String> DEFAULT_PROCESS_SEQUENCE_TYPES = List.of("PROCESS_SEQUENCE");
    private static final List<String> DEFAULT_TASK_STEP_RELATIONS = List.of(
            MetaforgeLibraryFqns.Relation.TASK_HAS_ENTRY_STEP);
    private static final List<String> DEFAULT_TASK_DECISION_STEP_RELATIONS = List.of(
            MetaforgeLibraryFqns.Relation.TASK_HAS_ENTRY_DECISION_STEP);
    private static final List<String> DEFAULT_TASK_SUBTASK_RELATIONS = List.of(
            MetaforgeLibraryFqns.Relation.TASK_HAS_ENTRY_SUBTASK);

    @Override
    public String operatorId() {
        return "procedural.flow-blueprint";
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
        String entryStepFqn = resolveEntryStep(entityFqn, config);

        List<Map<String, Object>> path = buildStepSequence(entryStepFqn, config);
        // annotated_path 为 path 的注解增强版（含 marker ENTRY/STEP/EXIT），信息完整，故仅输出 annotated_path
        List<Map<String, Object>> annotatedPath = annotateSteps(path, config);

        Map<String, Object> resultData = new LinkedHashMap<>();
        resultData.put("annotated_path", annotatedPath);
        resultData.put("length", annotatedPath.size());
        resultData.put("entityFqn", entityFqn);
        resultData.put("entryStepFqn", entryStepFqn);

        return CognitionResult.success(operatorId(), category(), resultData);
    }

    private String resolveEntryStep(String entityFqn, Map<String, Object> config) {
        String taskSchema = configString(config, "taskEntitySchemaFqn", MetaforgeLibraryFqns.Entity.TASK);
        Object entityResult = executeWithPort(() -> metadataReadPort.getByFqn(entityFqn));
        if (!(entityResult instanceof MetadataEntityDto dto)) {
            return entityFqn;
        }
        if (taskSchema != null && !taskSchema.isBlank() && taskSchema.equals(dto.getEntitySchemaFqn())) {
            Object entry = resolveContentValue(dto, "entry_step_fqn");
            if (entry instanceof String s && !s.isBlank()) {
                return s;
            }
            return findEntryStep(entityFqn, config);
        }
        return entityFqn;
    }

    private String findEntryStep(String taskFqn, Map<String, Object> config) {
        // 1. 普通起点步骤：TaskHasEntryStep 找 ENTRY
        Object relResult = executeWithPort(() -> queryRelationsBySchema(
                taskFqn, RelationDirection.OUTBOUND, config, DEFAULT_TASK_STEP_RELATIONS, null));
        if (relResult instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof RelationInstanceDto dto) {
                    String stepFqn = dto.getTargetEntityFqn();
                    Object stepResult = executeWithPort(() -> metadataReadPort.getByFqn(stepFqn));
                    if (stepResult instanceof MetadataEntityDto sdto) {
                        Object stepType = resolveContentValue(sdto, "step_type");
                        if ("ENTRY".equals(stepType)) {
                            return stepFqn;
                        }
                    }
                }
            }
        }

        // 2. 起点决策步骤：TaskHasEntryDecisionStep，直接作为入口（决策步骤本身是执行单元）
        Map<String, Object> decisionConfig = new LinkedHashMap<>();
        decisionConfig.put("relationSchemaFqn", MetaforgeLibraryFqns.Relation.TASK_HAS_ENTRY_DECISION_STEP);
        Object decisionResult = executeWithPort(() -> queryRelationsBySchema(
                taskFqn, RelationDirection.OUTBOUND, decisionConfig, DEFAULT_TASK_DECISION_STEP_RELATIONS, null));
        if (decisionResult instanceof List<?> decisionList) {
            for (Object item : decisionList) {
                if (item instanceof RelationInstanceDto dto && dto.getTargetEntityFqn() != null) {
                    return dto.getTargetEntityFqn();
                }
            }
        }

        // 3. 起点子任务：TaskHasEntrySubtask，递归解析子任务的入口步骤
        Map<String, Object> subtaskConfig = new LinkedHashMap<>();
        subtaskConfig.put("relationSchemaFqn", MetaforgeLibraryFqns.Relation.TASK_HAS_ENTRY_SUBTASK);
        Object subResult = executeWithPort(() -> queryRelationsBySchema(
                taskFqn, RelationDirection.OUTBOUND, subtaskConfig, DEFAULT_TASK_SUBTASK_RELATIONS, null));
        if (subResult instanceof List<?> subList) {
            for (Object item : subList) {
                if (item instanceof RelationInstanceDto dto && dto.getTargetEntityFqn() != null) {
                    String subtaskFqn = dto.getTargetEntityFqn();
                    String subEntry = resolveEntryStep(subtaskFqn, config);
                    if (subEntry != null && !subEntry.isBlank() && !subtaskFqn.equals(subEntry)) {
                        return subEntry;
                    }
                }
            }
        }
        return taskFqn;
    }

    /**
     * 从入口步沿 StepHasNextStep 逐跳遍历，构建步骤节点序列（含入口步）。
     * 每个节点：{fqn, relationType(进入该节点的关系类型), sourceEntityFqn, targetEntityFqn}。
     */
    private List<Map<String, Object>> buildStepSequence(String entryFqn, Map<String, Object> config) {
        List<Map<String, Object>> path = new ArrayList<>();
        if (entryFqn == null || entryFqn.isBlank()) {
            return path;
        }

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("fqn", entryFqn);
        entry.put("sourceEntityFqn", null);
        entry.put("targetEntityFqn", entryFqn);
        entry.put("relationType", null);
        enrichStep(entry);
        path.add(entry);

        Set<String> visited = new HashSet<>();
        visited.add(entryFqn);

        String current = entryFqn;
        int maxHops = resolveMaxHops(config);
        for (int hop = 0; hop < maxHops; hop++) {
            final String from = current;
            Object relResult = executeWithPort(() -> queryProcessSequenceRelations(from, config));
            if (relResult instanceof CognitionResult) {
                break;
            }
            if (!(relResult instanceof List<?> list) || list.isEmpty()) {
                break;
            }
            String next = null;
            for (Object item : list) {
                if (item instanceof RelationInstanceDto dto && dto.getTargetEntityFqn() != null) {
                    next = dto.getTargetEntityFqn();
                    break;
                }
            }
            if (next == null || visited.contains(next)) {
                break;
            }
            Map<String, Object> step = new LinkedHashMap<>();
            step.put("fqn", next);
            step.put("sourceEntityFqn", current);
            step.put("targetEntityFqn", next);
            Object relType = null;
            if (list.get(0) instanceof RelationInstanceDto firstRel) {
                relType = firstRel.getRelationType();
            }
            step.put("relationType", relType);
            enrichStep(step);
            path.add(step);
            visited.add(next);
            current = next;
        }
        return path;
    }

    /**
     * 以 relationType=PROCESS_SEQUENCE 统一查询执行单元的流程后继，
     * 自动覆盖所有流程关系：StepHasNextStep/StepHasNextDecisionStep/StepHasNextTask、
     * DecisionStepHasNextStep/DecisionStepHasNextDecisionStep/DecisionStepHasNextTask、TaskHasNextStep。
     */
    private List<RelationInstanceDto> queryProcessSequenceRelations(String fqn, Map<String, Object> config) {
        Map<String, Object> localConfig = new LinkedHashMap<>();
        localConfig.put("relationTypes", DEFAULT_PROCESS_SEQUENCE_TYPES);
        localConfig.put("relationSchemaFqns", List.of());
        localConfig.put("relationSchemaFqnPrefix", "");
        return queryRelationsBySchema(fqn, RelationDirection.OUTBOUND, localConfig, List.of(), null);
    }

    /**
     * 为步骤节点补充元数据实体摘要（name/description）——步骤节点视为实体，通用要求含 fqn/name/description。
     */
    private void enrichStep(Map<String, Object> step) {
        Object fqn = step.get("fqn");
        if (!(fqn instanceof String s) || s.isBlank()) {
            return;
        }
        Object meta = executeWithPort(() -> metadataReadPort.getByFqn(s));
        if (meta instanceof MetadataEntityDto dto) {
            Map<String, Object> summary = toEntitySummary(dto);
            step.putIfAbsent("name", summary.get("name"));
            step.putIfAbsent("description", summary.get("description"));
            step.putIfAbsent("entitySchemaFqn", summary.get("entitySchemaFqn"));
        }
    }

    private int resolveMaxHops(Map<String, Object> config) {
        Object value = config != null ? config.get("maxHops") : null;
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof String s && !s.isBlank()) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return DEFAULT_MAX_HOPS;
    }

    private List<Map<String, Object>> annotateSteps(List<Map<String, Object>> path, Map<String, Object> config) {
        List<Map<String, Object>> annotated = new ArrayList<>();
        if (path == null || path.isEmpty()) {
            return annotated;
        }
        for (int i = 0; i < path.size(); i++) {
            Map<String, Object> step = new LinkedHashMap<>(path.get(i));
            if (i == 0) {
                step.put("marker", "ENTRY");
            } else if (i == path.size() - 1) {
                step.put("marker", "EXIT");
            } else {
                String fqn = (String) path.get(i).get("fqn");
                int outboundCount = countOutboundEdges(fqn, config);
                step.put("marker", outboundCount > 1 ? "DECISION" : "STEP");
            }
            annotated.add(step);
        }
        return annotated;
    }

    private int countOutboundEdges(String fqn, Map<String, Object> config) {
        if (fqn == null) return 0;
        Object result = executeWithPort(() -> queryProcessSequenceRelations(fqn, config));
        if (result instanceof List<?> list) return list.size();
        return 0;
    }
}
