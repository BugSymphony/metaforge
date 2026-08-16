package com.metaforge.agent.cognition.api.exception;

import com.metaforge.agent.cognition.api.constants.AgentCognitionErrorCodes;
import com.metaforge.common.exception.BizException;

public class InvalidOperatorSelectionException extends BizException {

    public InvalidOperatorSelectionException(java.util.Set<String> operators, String templateId) {
        super(AgentCognitionErrorCodes.INVALID_OPERATOR_SELECTION,
                "请求的 operators " + operators + " 无任何算子匹配模板 " + templateId + " 的声明");
    }
}
