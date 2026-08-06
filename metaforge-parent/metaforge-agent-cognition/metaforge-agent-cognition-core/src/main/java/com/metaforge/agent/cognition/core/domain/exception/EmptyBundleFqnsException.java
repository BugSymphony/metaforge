package com.metaforge.agent.cognition.core.domain.exception;

import com.metaforge.agent.cognition.api.constant.AgentCognitionErrorCodes;

public class EmptyBundleFqnsException extends AgentCognitionBizException {

    public EmptyBundleFqnsException(String message) {
        super(AgentCognitionErrorCodes.EMPTY_BUNDLE_FQNS, message);
    }

    public EmptyBundleFqnsException(String message, Throwable cause) {
        super(AgentCognitionErrorCodes.EMPTY_BUNDLE_FQNS, message, cause);
    }

    @Override
    public String getErrorCodeName() { return "EMPTY_BUNDLE_FQNS"; }
}
