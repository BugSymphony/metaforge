package com.metaforge.agent.cognition.core.application.executor.impl;

import com.metaforge.agent.cognition.api.enums.PerspectiveCode;
import com.metaforge.agent.cognition.api.perspective.PerspectiveExecutionContext;
import com.metaforge.agent.cognition.api.perspective.PerspectiveExecutor;
import com.metaforge.agent.cognition.core.domain.model.entity.FlowBlueprint;
import com.metaforge.graph.api.dto.RelationInstanceDto;
import com.metaforge.metadata.api.dto.response.MetadataEntityDto;
import com.metaforge.metamodel.api.dto.response.EntitySchemaDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FlowBlueprintExecutor implements PerspectiveExecutor {

    private static final Logger log = LoggerFactory.getLogger(FlowBlueprintExecutor.class);

    private final ExecutorSupport support;

    public FlowBlueprintExecutor(ExecutorSupport support) {
        this.support = support;
    }

    @Override
    public PerspectiveCode supportedPerspective() {
        return PerspectiveCode.FLOW_BLUEPRINT;
    }

    @Override
    public Object execute(PerspectiveExecutionContext ctx) {
        log.debug("执行流程蓝图视角: bundleFqns={}", ctx.bundleFqns());

        FlowBlueprint blueprint = new FlowBlueprint();
        blueprint.setBundleFqn(ctx.bundleFqns() != null && !ctx.bundleFqns().isEmpty()
                ? ctx.bundleFqns().get(0) : "");
        blueprint.setSteps(new ArrayList<>());
        blueprint.setExitSteps(new ArrayList<>());
        blueprint.setBranchPoints(new ArrayList<>());
        blueprint.setEmpty(false);

        if (ctx.bundleFqns() == null || ctx.bundleFqns().isEmpty()) {
            blueprint.setEmpty(true);
            blueprint.setEmptyNote("未指定 Bundle FQN，无法查询流程蓝图");
            return blueprint;
        }

        List<FlowBlueprint.FlowStep> steps = new ArrayList<>();
        for (String bundleCode : ctx.bundleFqns()) {
            String prefix = support.resolveVersionedPrefix(bundleCode);
            if (prefix == null) {
                continue;
            }
            Object rawSchemas = support.metamodel().listEntitySchemasByPrefixes(List.of(prefix));
            for (EntitySchemaDto schema : support.schemas(rawSchemas)) {
                if (!"ExecutionStep".equals(schema.getName())) {
                    continue;
                }
                Object rawEntities = support.metadata().listByEntitySchema(schema.getFqn(), 1, Integer.MAX_VALUE);
                int order = 1;
                for (MetadataEntityDto entity : support.entities(rawEntities)) {
                    FlowBlueprint.FlowStep step = new FlowBlueprint.FlowStep();
                    step.setStepFqn(entity.getFqn());
                    step.setName(entity.getName() != null ? entity.getName() : entity.getFqn());
                    step.setDescription(entity.getDescription());
                    step.setPreconditions(new ArrayList<>());
                    step.setOutputs(new ArrayList<>());
                    step.setSequenceOrder(order++);
                    steps.add(step);
                }
            }
        }

        blueprint.setSteps(steps);
        if (steps.isEmpty()) {
            blueprint.setEmpty(true);
            blueprint.setEmptyNote("当前 Bundle 未定义 ExecutionStep 流程");
        }
        return blueprint;
    }
}
