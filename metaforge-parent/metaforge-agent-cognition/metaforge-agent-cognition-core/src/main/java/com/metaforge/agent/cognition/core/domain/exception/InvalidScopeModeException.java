package com.metaforge.agent.cognition.core.domain.exception;

import com.metaforge.agent.cognition.api.constant.AgentCognitionErrorCodes;

public class InvalidScopeModeException extends AgentCognitionBizException {

    public InvalidScopeModeException(String message) {
        super(AgentCognitionErrorCodes.SCOPE_MODE_INVALID, message);
    }

    public InvalidScopeModeException(String message, Throwable cause) {
        super(AgentCognitionErrorCodes.SCOPE_MODE_INVALID, message, cause);
    }

    @Override
    public String getErrorCodeName() { return "SCOPE_MODE_INVALID"; }
}
