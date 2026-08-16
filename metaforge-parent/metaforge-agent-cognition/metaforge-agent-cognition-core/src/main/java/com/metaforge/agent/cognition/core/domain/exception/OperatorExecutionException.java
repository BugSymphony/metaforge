package com.metaforge.agent.cognition.core.domain.exception;

import com.metaforge.agent.cognition.api.constants.AgentCognitionErrorCodes;
import com.metaforge.common.exception.BizException;

public class OperatorExecutionException extends BizException {

    public OperatorExecutionException(String operatorId, String message) {
        super(AgentCognitionErrorCodes.OPERATOR_EXECUTION_ERROR,
                "算子 " + operatorId + " 执行异常: " + message);
    }

    public OperatorExecutionException(String operatorId, Throwable cause) {
        super(AgentCognitionErrorCodes.OPERATOR_EXECUTION_ERROR,
                "算子 " + operatorId + " 执行异常: " + cause.getMessage(), cause);
    }
}
