package com.metaforge.agent.cognition.core.domain.exception;

import com.metaforge.common.exception.BizException;

public abstract class AgentCognitionBizException extends BizException {

    protected AgentCognitionBizException(int code, String message) {
        super(code, message);
    }

    protected AgentCognitionBizException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }

    protected AgentCognitionBizException(int code, String message, Object data) {
        super(code, message, data);
    }

    public abstract String getErrorCodeName();
}
