package com.metaforge.agent.cognition.core.domain.exception;

import com.metaforge.agent.cognition.api.constant.AgentCognitionErrorCodes;

public class InvalidEntityFqnException extends AgentCognitionBizException {

    public InvalidEntityFqnException(String message) {
        super(AgentCognitionErrorCodes.INVALID_ENTITY_FQN, message);
    }

    public InvalidEntityFqnException(String message, Throwable cause) {
        super(AgentCognitionErrorCodes.INVALID_ENTITY_FQN, message, cause);
    }

    @Override
    public String getErrorCodeName() { return "INVALID_ENTITY_FQN"; }
}
