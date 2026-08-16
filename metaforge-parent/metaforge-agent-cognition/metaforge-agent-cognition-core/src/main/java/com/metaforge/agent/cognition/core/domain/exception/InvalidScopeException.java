package com.metaforge.agent.cognition.core.domain.exception;

import com.metaforge.agent.cognition.api.constants.AgentCognitionErrorCodes;
import com.metaforge.common.exception.BizException;

public class InvalidScopeException extends BizException {

    public InvalidScopeException(String detail) {
        super(AgentCognitionErrorCodes.INVALID_SCOPE,
                "scope 无效: " + detail);
    }
}
