package com.metaforge.agent.cognition.core.domain.exception;

import com.metaforge.agent.cognition.api.constant.AgentCognitionErrorCodes;

public class PerspectiveTimeoutException extends AgentCognitionBizException {

    public PerspectiveTimeoutException(String message) {
        super(AgentCognitionErrorCodes.PERSPECTIVE_TIMEOUT, message);
    }

    public PerspectiveTimeoutException(String message, Throwable cause) {
        super(AgentCognitionErrorCodes.PERSPECTIVE_TIMEOUT, message, cause);
    }

    @Override
    public String getErrorCodeName() { return "PERSPECTIVE_TIMEOUT"; }
}
