package com.metaforge.agent.cognition.api.perspective;

import com.metaforge.agent.cognition.api.enums.PerspectiveCode;

public interface PerspectiveExecutor {

    PerspectiveCode supportedPerspective();

    Object execute(PerspectiveExecutionContext ctx);
}
