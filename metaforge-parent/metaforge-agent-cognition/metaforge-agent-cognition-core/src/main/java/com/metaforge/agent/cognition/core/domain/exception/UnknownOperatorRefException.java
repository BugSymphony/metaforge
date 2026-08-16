package com.metaforge.agent.cognition.core.domain.exception;

import com.metaforge.agent.cognition.api.constants.AgentCognitionErrorCodes;
import com.metaforge.common.exception.BizException;

public class UnknownOperatorRefException extends BizException {

    public UnknownOperatorRefException(String operatorId) {
        super(AgentCognitionErrorCodes.UNKNOWN_OPERATOR_REF,
                "算子 " + operatorId + " 引用的分类前缀不存在");
    }
}
