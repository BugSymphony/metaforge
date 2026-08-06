package com.metaforge.agent.cognition.core.application.executor.impl;

import com.metaforge.agent.cognition.api.enums.ContextMode;
import com.metaforge.agent.cognition.api.enums.PerspectiveCode;
import com.metaforge.agent.cognition.api.perspective.PerspectiveExecutionContext;
import com.metaforge.agent.cognition.api.perspective.PerspectiveExecutor;
import com.metaforge.agent.cognition.core.domain.model.entity.ConstraintSet;
import com.metaforge.metadata.api.dto.response.MetadataEntityDto;
import com.metaforge.metamodel.api.dto.NativeAttributeDto;
import com.metaforge.metamodel.api.dto.response.EntitySchemaDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ConstraintSetExecutor implements PerspectiveExecutor {

    private static final Logger log = LoggerFactory.getLogger(ConstraintSetExecutor.class);

    private final ExecutorSupport support;

    public ConstraintSetExecutor(ExecutorSupport support) {
        this.support = support;
    }

    @Override
    public PerspectiveCode supportedPerspective() {
        return PerspectiveCode.CONSTRAINT_SET;
    }

    @Override
    public Object execute(PerspectiveExecutionContext ctx) {
        log.debug("执行约束规则视角: entityFqn={}, contextMode={}", ctx.entityFqn(), ctx.contextMode());

        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.setConstraints(new ArrayList<>());
        constraintSet.setHardBoundaries(new ArrayList<>());
        constraintSet.setSoftBoundaries(new ArrayList<>());
        constraintSet.setEmpty(false);

        if (ctx.entityFqn() != null) {
            Object raw = support.metadata().getByFqn(ctx.entityFqn());
            if (raw instanceof MetadataEntityDto entity && entity.getEntitySchemaFqn() != null) {
                buildHardBoundaries(entity.getEntitySchemaFqn(), constraintSet);
            }
        } else if (ctx.contextMode() == ContextMode.BUNDLE_LEVEL && ctx.bundleFqns() != null) {
            for (String bundleCode : ctx.bundleFqns()) {
                String prefix = support.resolveVersionedPrefix(bundleCode);
                if (prefix == null) {
                    continue;
                }
                Object rawSchemas = support.metamodel().listEntitySchemasByPrefixes(List.of(prefix));
                for (EntitySchemaDto schema : support.schemas(rawSchemas)) {
                    buildHardBoundaries(schema.getFqn(), constraintSet);
                }
            }
        }

        boolean hasData = !constraintSet.getConstraints().isEmpty()
                || !constraintSet.getHardBoundaries().isEmpty()
                || !constraintSet.getSoftBoundaries().isEmpty();
        constraintSet.setEmpty(!hasData);
        if (!hasData) {
            constraintSet.setEmptyNote("未找到可用的约束规则");
        }

        return constraintSet;
    }

    private void buildHardBoundaries(String schemaFqn, ConstraintSet constraintSet) {
        Object raw = support.metamodel().getEntitySchema(schemaFqn);
        if (!(raw instanceof EntitySchemaDto schema)) {
            return;
        }
        for (NativeAttributeDto attr : support.parseNativeAttributes(schema.getNativeAttributes())) {
            ConstraintSet.HardBoundary boundary = new ConstraintSet.HardBoundary();
            boundary.setFieldName(attr.getName());
            boundary.setRequired(attr.isRequired());
            Map<String, Object> constraints = attr.getConstraints();
            if (constraints != null) {
                Object enumVal = constraints.get("enum");
                if (enumVal instanceof List<?> list) {
                    List<String> enumValues = new ArrayList<>();
                    for (Object o : list) {
                        enumValues.add(String.valueOf(o));
                    }
                    boundary.setEnumValues(enumValues);
                }
                Object min = constraints.get("minimum");
                if (min != null) {
                    boundary.setMinimum(min);
                }
                Object max = constraints.get("maximum");
                if (max != null) {
                    boundary.setMaximum(max);
                }
                Object pattern = constraints.get("pattern");
                if (pattern != null) {
                    boundary.setPattern(String.valueOf(pattern));
                }
            }
            constraintSet.getHardBoundaries().add(boundary);
        }
    }
}
