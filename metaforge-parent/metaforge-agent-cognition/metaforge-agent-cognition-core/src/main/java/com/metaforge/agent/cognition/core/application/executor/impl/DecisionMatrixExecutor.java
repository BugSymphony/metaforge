package com.metaforge.agent.cognition.core.application.executor.impl;

import com.metaforge.agent.cognition.api.enums.ContextMode;
import com.metaforge.agent.cognition.api.enums.PerspectiveCode;
import com.metaforge.agent.cognition.api.perspective.PerspectiveExecutionContext;
import com.metaforge.agent.cognition.api.perspective.PerspectiveExecutor;
import com.metaforge.agent.cognition.core.domain.model.entity.DecisionMatrix;
import com.metaforge.metadata.api.dto.response.MetadataEntityDto;
import com.metaforge.metamodel.api.dto.response.EntitySchemaDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DecisionMatrixExecutor implements PerspectiveExecutor {

    private static final Logger log = LoggerFactory.getLogger(DecisionMatrixExecutor.class);

    private final ExecutorSupport support;

    public DecisionMatrixExecutor(ExecutorSupport support) {
        this.support = support;
    }

    @Override
    public PerspectiveCode supportedPerspective() {
        return PerspectiveCode.DECISION_MATRIX;
    }

    @Override
    public Object execute(PerspectiveExecutionContext ctx) {
        log.debug("执行决策图谱视角: entityFqn={}, contextMode={}", ctx.entityFqn(), ctx.contextMode());

        DecisionMatrix matrix = new DecisionMatrix();
        matrix.setDecisionPoints(new ArrayList<>());

        List<String> schemaFqns = findSchemaFqns(ctx, "DecisionRule");
        if (schemaFqns.isEmpty()) {
            return matrix;
        }

        List<DecisionMatrix.DecisionPoint> points = new ArrayList<>();
        for (String schemaFqn : schemaFqns) {
            Object raw = support.metadata().listByEntitySchema(schemaFqn, 1, Integer.MAX_VALUE);
            for (MetadataEntityDto entity : support.entities(raw)) {
                DecisionMatrix.DecisionPoint point = new DecisionMatrix.DecisionPoint();
                point.setDecisionEntityFqn(entity.getFqn());
                point.setDecisionName(entity.getName() != null ? entity.getName() : entity.getFqn());
                point.setRecommendation(stringifyContent(entity, "recommended_option"));
                point.setOptions(buildOptions(entity));
                points.add(point);
            }
        }
        matrix.setDecisionPoints(points);
        return matrix;
    }

    private List<String> findSchemaFqns(PerspectiveExecutionContext ctx, String schemaSimpleName) {
        List<String> result = new ArrayList<>();
        if (ctx.bundleFqns() == null) {
            return result;
        }
        for (String bundleCode : ctx.bundleFqns()) {
            String prefix = support.resolveVersionedPrefix(bundleCode);
            if (prefix == null) {
                continue;
            }
            Object rawSchemas = support.metamodel().listEntitySchemasByPrefixes(List.of(prefix));
            for (EntitySchemaDto schema : support.schemas(rawSchemas)) {
                if (schema.getName() != null && schema.getName().equals(schemaSimpleName)) {
                    result.add(schema.getFqn());
                }
            }
        }
        return result;
    }

    private List<DecisionMatrix.DecisionPoint.DecisionOption> buildOptions(MetadataEntityDto entity) {
        List<DecisionMatrix.DecisionPoint.DecisionOption> options = new ArrayList<>();
        for (com.metaforge.computeengine.api.dto.common.RelationSummary relation
                : support.outboundRelations(entity.getFqn())) {
            if (relation.targetEntityFqn() == null) {
                continue;
            }
            DecisionMatrix.DecisionPoint.DecisionOption option =
                    new DecisionMatrix.DecisionPoint.DecisionOption();
            option.setTargetEntityFqn(relation.targetEntityFqn());
            option.setTriggerCondition(null);
            option.setDownstreamImpact(new ArrayList<>());
            options.add(option);
        }
        return options;
    }

    private String stringifyContent(MetadataEntityDto entity, String key) {
        if (entity.getContent() == null) {
            return null;
        }
        Object value = entity.getContent().get(key);
        return value != null ? String.valueOf(value) : null;
    }
}
