package com.metaforge.agent.cognition.core.domain.exception;

import com.metaforge.agent.cognition.api.constants.AgentCognitionErrorCodes;
import com.metaforge.common.exception.BizException;

public class OperatorTimeoutException extends BizException {

    public OperatorTimeoutException(String operatorId, long timeoutMs) {
        super(AgentCognitionErrorCodes.OPERATOR_TIMEOUT,
                "算子 " + operatorId + " 执行超时 (超时时间: " + timeoutMs + "ms)");
    }
}
