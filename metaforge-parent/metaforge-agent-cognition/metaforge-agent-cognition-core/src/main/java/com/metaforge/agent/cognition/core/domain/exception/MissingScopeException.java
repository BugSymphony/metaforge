package com.metaforge.agent.cognition.core.domain.exception;

import com.metaforge.agent.cognition.api.constants.AgentCognitionErrorCodes;
import com.metaforge.common.exception.BizException;

public class MissingScopeException extends BizException {

    public MissingScopeException(String templateId) {
        super(AgentCognitionErrorCodes.MISSING_SCOPE,
                "模板 " + templateId + " 要求 scope 但请求未提供");
    }
}
