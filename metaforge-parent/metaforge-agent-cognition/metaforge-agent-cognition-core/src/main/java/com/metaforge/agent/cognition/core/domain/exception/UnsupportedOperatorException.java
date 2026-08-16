package com.metaforge.agent.cognition.core.domain.exception;

import com.metaforge.agent.cognition.api.constants.AgentCognitionErrorCodes;
import com.metaforge.common.exception.BizException;

public class UnsupportedOperatorException extends BizException {

    public UnsupportedOperatorException(String operatorId) {
        super(AgentCognitionErrorCodes.UNSUPPORTED_OPERATOR,
                "算子 " + operatorId + " 无匹配注册算子");
    }
}
