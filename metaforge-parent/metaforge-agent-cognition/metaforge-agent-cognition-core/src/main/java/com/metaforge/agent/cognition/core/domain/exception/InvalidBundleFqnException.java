package com.metaforge.agent.cognition.core.domain.exception;

import com.metaforge.agent.cognition.api.constant.AgentCognitionErrorCodes;

public class InvalidBundleFqnException extends AgentCognitionBizException {

    public InvalidBundleFqnException(String message) {
        super(AgentCognitionErrorCodes.INVALID_BUNDLE_FQN, message);
    }

    public InvalidBundleFqnException(String message, Throwable cause) {
        super(AgentCognitionErrorCodes.INVALID_BUNDLE_FQN, message, cause);
    }

    @Override
    public String getErrorCodeName() { return "INVALID_BUNDLE_FQN"; }
}
