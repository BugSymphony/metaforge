package com.metaforge.agent.cognition.core.domain.exception;

import com.metaforge.agent.cognition.api.constant.AgentCognitionErrorCodes;

public class TemplateNotFoundException extends AgentCognitionBizException {

    public TemplateNotFoundException(String message) {
        super(AgentCognitionErrorCodes.TEMPLATE_NOT_FOUND, message);
    }

    public TemplateNotFoundException(String message, Throwable cause) {
        super(AgentCognitionErrorCodes.TEMPLATE_NOT_FOUND, message, cause);
    }

    @Override
    public String getErrorCodeName() { return "TEMPLATE_NOT_FOUND"; }
}
