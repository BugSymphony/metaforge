package com.metaforge.agent.cognition.operator.governance;

import com.metaforge.agent.cognition.api.enums.DimensionCategory;
import com.metaforge.agent.cognition.api.spi.CognitionQueryContext;
import com.metaforge.agent.cognition.api.spi.CognitionResult;
import com.metaforge.agent.cognition.operator.common.AbstractCognitionOperator;
import com.metaforge.agent.cognition.operator.common.MetaforgeLibraryFqns;
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
 * 三层收窄——(1)蓝图沿 {@code StepHasNextStep} 前后遍历；(2)收集关联实体
 * （能力 {@code StepUsesCapability}、规则 {@code RuleAppliesTo}、决策步骤 {@code TaskHasEntryDecisionStep} 及后继）；
 * (3)反查 entity_schema 去重。
 */
@Component
public class GovernanceScopeNarrowingOperator extends AbstractCognitionOperator {

    private static final List<String> DEFAULT_SEQUENCE_RELATIONS = List.of(
            MetaforgeLibraryFqns.Relation.STEP_HAS_NEXT_STEP);
    private static final List<String> DEFAULT_PROCESS_SEQUENCE_TYPES = List.of("PROCESS_SEQUENCE");
    private static final List<String> DEFAULT_TASK_ENTRY_RELATIONS = List.of(
            MetaforgeLibraryFqns.Relation.TASK_HAS_ENTRY_STEP,
            MetaforgeLibraryFqns.Relation.TASK_HAS_ENTRY_DECISION_STEP,
            MetaforgeLibraryFqns.Relation.TASK_HAS_ENTRY_SUBTASK);
    private static final int DEFAULT_MAX_ROUNDS = 4;
    private static final List<String> DEFAULT_TASK_STEP_RELATIONS = List.of(
            MetaforgeLibraryFqns.Relation.TASK_HAS_ENTRY_STEP);
    private static final List<String> DEFAULT_DECISION_STEP_RELATIONS = List.of(
            MetaforgeLibraryFqns.Relation.TASK_HAS_ENTRY_DECISION_STEP);
    private static final List<String> DEFAULT_DECISION_SUCCESSOR_RELATIONS = List.of(
            MetaforgeLibraryFqns.Relation.DECISION_STEP_HAS_NEXT_STEP,
            MetaforgeLibraryFqns.Relation.DECISION_STEP_HAS_NEXT_DECISION_STEP,
            MetaforgeLibraryFqns.Relation.DECISION_STEP_HAS_NEXT_TASK);

    @Override
    public String operatorId() {
        return "governance.scope-narrowing";
    }

    @Override
    public DimensionCategory category() {
        return DimensionCategory.GOVERNANCE;
    }

    @Override
    public CognitionResult execute(CognitionQueryContext context) {
        String entryFqn = context.entityFqn();
        if (entryFqn == null || entryFqn.isBlank()) {
            return wrapFailure("缺少 entry_entity_fqn 参数");
        }

        Map<String, Object> config = context.operatorConfig();

        Set<String> blueprintFqns = narrowBlueprint(entryFqn, config);

        Set<String> entityFqns = collectEntityFqns(blueprintFqns, config);

        Set<String> schemas = deduplicateSchemas(entityFqns);

        Map<String, Object> resultData = new LinkedHashMap<>();
        resultData.put("blueprint_scope", new ArrayList<>(blueprintFqns));
        resultData.put("entityFqns", new ArrayList<>(entityFqns));
        resultData.put("schemas", new ArrayList<>(schemas));
        resultData.put("entryFqn", entryFqn);

        // 产出 updated_scope（收窄后的认知边界），供 DELEGATE 聚合到响应顶层
        Map<String, Object> updatedScope = new LinkedHashMap<>();
        updatedScope.put("bundles", resolveScopeBundles(context));
        updatedScope.put("entity_schemas", new ArrayList<>(schemas));
        resultData.put("updated_scope", updatedScope);

        return CognitionResult.success(operatorId(), category(), resultData);
    }

    private List<String> resolveScopeBundles(CognitionQueryContext context) {
        com.metaforge.agent.cognition.api.dto.request.Scope scope = context.scope();
        return scope != null && scope.bundles() != null ? scope.bundles() : java.util.Collections.emptyList();
    }

    private Set<String> narrowBlueprint(String entryFqn, Map<String, Object> config) {
        Set<String> fqns = new LinkedHashSet<>();
        fqns.add(entryFqn);

        int maxRounds = resolveMaxRounds(config);
        for (int round = 0; round < maxRounds; round++) {
            Set<String> extended = new LinkedHashSet<>(fqns);
            for (String fqn : fqns) {
                // 沿 PROCESS_SEQUENCE 前后遍历（覆盖 Step/DecisionStep/Task 全部流程关系）
                extended.addAll(resolveProcessAdjacentFqns(fqn, config));
                // Task 锚点展开入口执行单元（TaskHasEntryStep/DecisionStep/Subtask）
                extended.addAll(resolveTaskEntryFqns(fqn, config));
            }
            if (extended.size() == fqns.size()) {
                break;
            }
            fqns = extended;
        }
        return fqns;
    }

    /**
     * PROCESS_SEQUENCE 类型的前后 1 度邻域——自动覆盖
     * StepHasNextStep/StepHasNextDecisionStep/StepHasNextTask、
     * DecisionStepHasNextStep/DecisionStepHasNextDecisionStep/DecisionStepHasNextTask、TaskHasNextStep。
     */
    private List<String> resolveProcessAdjacentFqns(String fqn, Map<String, Object> config) {
        Set<String> adjacent = new LinkedHashSet<>();
        for (RelationInstanceDto dto : queryProcessSequenceRelations(fqn, RelationDirection.OUTBOUND)) {
            if (dto.getTargetEntityFqn() != null) {
                adjacent.add(dto.getTargetEntityFqn());
            }
        }
        for (RelationInstanceDto dto : queryProcessSequenceRelations(fqn, RelationDirection.INBOUND)) {
            if (dto.getSourceEntityFqn() != null) {
                adjacent.add(dto.getSourceEntityFqn());
            }
        }
        return new ArrayList<>(adjacent);
    }

    private List<RelationInstanceDto> queryProcessSequenceRelations(String fqn, RelationDirection direction) {
        Map<String, Object> localConfig = new LinkedHashMap<>();
        localConfig.put("relationTypes", DEFAULT_PROCESS_SEQUENCE_TYPES);
        localConfig.put("relationSchemaFqns", List.of());
        localConfig.put("relationSchemaFqnPrefix", "");
        Object result = executeWithPort(() -> queryRelationsBySchema(
                fqn, direction, localConfig, List.of(), null));
        if (!(result instanceof List<?> list)) {
            return new ArrayList<>();
        }
        List<RelationInstanceDto> out = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof RelationInstanceDto dto) {
                out.add(dto);
            }
        }
        return out;
    }

    /**
     * Task 锚点展开入口执行单元：TaskHasEntryStep/TaskHasEntryDecisionStep/TaskHasEntrySubtask。
     */
    private List<String> resolveTaskEntryFqns(String fqn, Map<String, Object> config) {
        List<String> entries = new ArrayList<>();
        Object entity = executeWithPort(() -> metadataReadPort.getByFqn(fqn));
        if (!(entity instanceof MetadataEntityDto dto)
                || !MetaforgeLibraryFqns.Entity.TASK.equals(dto.getEntitySchemaFqn())) {
            return entries;
        }
        Object relResult = executeWithPort(() -> queryRelationsBySchema(
                fqn, RelationDirection.OUTBOUND, config, DEFAULT_TASK_ENTRY_RELATIONS, null));
        if (relResult instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof RelationInstanceDto rdto && rdto.getTargetEntityFqn() != null) {
                    entries.add(rdto.getTargetEntityFqn());
                }
            }
        }
        return entries;
    }

    private int resolveMaxRounds(Map<String, Object> config) {
        Object value = config != null ? config.get("maxRounds") : null;
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof String s && !s.isBlank()) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return DEFAULT_MAX_ROUNDS;
    }

    private Set<String> collectEntityFqns(Set<String> blueprintFqns, Map<String, Object> config) {
        Set<String> fqns = new LinkedHashSet<>(blueprintFqns);

        for (String bfqn : blueprintFqns) {
            // 能力：Step → Capability
            addPeerFqns(bfqn, RelationDirection.OUTBOUND, config,
                    List.of(MetaforgeLibraryFqns.Relation.STEP_USES_CAPABILITY), fqns);
            // 任务级能力：Task → Capability
            addPeerFqns(bfqn, RelationDirection.OUTBOUND, config,
                    List.of(MetaforgeLibraryFqns.Relation.TASK_REQUIRES_CAPABILITY), fqns);
            // 规则：Rule → Step（入边）
            addPeerFqns(bfqn, RelationDirection.INBOUND, config,
                    List.of(MetaforgeLibraryFqns.Relation.RULE_APPLIES_TO), fqns);
        }
        // 决策步骤 + 后继（通过所属任务）
        collectDecisionSteps(blueprintFqns, config, fqns);
        return fqns;
    }

    /**
     * 收集任务下的决策步骤及其后继：从蓝图步骤定位所属任务，
     * 查 TaskHasEntryDecisionStep 得决策步骤，再收其 DecisionStep 后继。
     */
    private void collectDecisionSteps(Set<String> blueprintFqns, Map<String, Object> config, Set<String> target) {
        Set<String> tasks = new LinkedHashSet<>();
        for (String bfqn : blueprintFqns) {
            String taskFqn = resolveTaskFqn(bfqn, config);
            if (taskFqn != null) {
                tasks.add(taskFqn);
            }
        }

        Set<String> decisionSteps = new LinkedHashSet<>();
        for (String taskFqn : tasks) {
            Object relResult = executeWithPort(() -> queryRelationsBySchema(
                    taskFqn, RelationDirection.OUTBOUND, config, DEFAULT_DECISION_STEP_RELATIONS, null));
            if (relResult instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof RelationInstanceDto dto && dto.getTargetEntityFqn() != null) {
                        decisionSteps.add(dto.getTargetEntityFqn());
                    }
                }
            }
        }
        target.addAll(decisionSteps);
        for (String dsFqn : decisionSteps) {
            addPeerFqns(dsFqn, RelationDirection.OUTBOUND, config, DEFAULT_DECISION_SUCCESSOR_RELATIONS, target);
        }
    }

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
        return null;
    }

    private void addPeerFqns(String fqn, RelationDirection direction, Map<String, Object> config,
                             List<String> relationSchemas, Set<String> target) {
        Object relResult = executeWithPort(() -> queryRelationsBySchema(
                fqn, direction, config, relationSchemas, null));
        if (relResult instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof RelationInstanceDto dto) {
                    String peer = resolvePeerFqn(dto, direction);
                    if (peer != null && !peer.isBlank()) {
                        target.add(peer);
                    }
                }
            }
        }
    }

    private Set<String> deduplicateSchemas(Set<String> entityFqns) {
        Set<String> schemaFqns = new LinkedHashSet<>();
        for (String fqn : entityFqns) {
            Object result = executeWithPort(() -> metadataReadPort.getByFqn(fqn));
            if (result instanceof MetadataEntityDto dto
                    && dto.getEntitySchemaFqn() != null && !dto.getEntitySchemaFqn().isBlank()) {
                schemaFqns.add(dto.getEntitySchemaFqn());
            }
        }
        return schemaFqns;
    }
}
