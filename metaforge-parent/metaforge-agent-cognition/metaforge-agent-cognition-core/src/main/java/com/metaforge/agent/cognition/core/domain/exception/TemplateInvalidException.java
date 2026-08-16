package com.metaforge.agent.cognition.core.domain.exception;

import com.metaforge.agent.cognition.api.constants.AgentCognitionErrorCodes;
import com.metaforge.common.exception.BizException;

public class TemplateInvalidException extends BizException {

    public TemplateInvalidException(String detail) {
        super(AgentCognitionErrorCodes.TEMPLATE_INVALID,
                "模板定义不合法: " + detail);
    }
}
