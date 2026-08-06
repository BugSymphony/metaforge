package com.metaforge.agent.cognition.core.domain.model.valueobject;

import java.util.List;

public record TemplateId(String value) {

    public static final TemplateId TASK_BRIEF = new TemplateId("task-brief");
    public static final TemplateId STEP_GUIDE = new TemplateId("step-guide");
    public static final TemplateId BUNDLE_CATALOG = new TemplateId("bundle-catalog");
    public static final TemplateId NAVIGATE = new TemplateId("navigate");
    public static final TemplateId COGNITION_GUIDANCE = new TemplateId("cognition-guidance");
    public static final TemplateId SUB_TASK_BRIEF = new TemplateId("sub-task-brief");

    public TemplateId {
        if (value == null || value.isBlank() || !List.of("task-brief", "step-guide",
                "bundle-catalog", "navigate", "cognition-guidance", "sub-task-brief").contains(value)) {
            throw new IllegalArgumentException("Invalid TemplateId: " + value);
        }
    }

    @Override
    public String toString() { return value; }
}
