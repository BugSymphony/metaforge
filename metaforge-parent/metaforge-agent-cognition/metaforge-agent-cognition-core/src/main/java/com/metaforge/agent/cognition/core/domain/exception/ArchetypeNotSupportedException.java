package com.metaforge.agent.cognition.core.domain.exception;

import com.metaforge.agent.cognition.api.constants.AgentCognitionErrorCodes;
import com.metaforge.common.exception.BizException;

public class ArchetypeNotSupportedException extends BizException {

    public ArchetypeNotSupportedException(String archetype, String templateId) {
        super(AgentCognitionErrorCodes.ARCHETYPE_NOT_SUPPORTED,
                "agentArchetype " + archetype + " 不被模板 " + templateId + " 的任一算子支持");
    }
}
