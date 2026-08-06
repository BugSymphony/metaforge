package com.metaforge.agent.cognition.core.domain.exception;

import com.metaforge.agent.cognition.api.constant.AgentCognitionErrorCodes;

public class UpstreamUnavailableException extends AgentCognitionBizException {

    public UpstreamUnavailableException(String message) {
        super(AgentCognitionErrorCodes.UPSTREAM_UNAVAILABLE, message);
    }

    public UpstreamUnavailableException(String message, Throwable cause) {
        super(AgentCognitionErrorCodes.UPSTREAM_UNAVAILABLE, message, cause);
    }

    @Override
    public String getErrorCodeName() { return "UPSTREAM_UNAVAILABLE"; }
}
