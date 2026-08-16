package com.metaforge.agent.cognition.core.domain.exception;

import com.metaforge.agent.cognition.api.constants.AgentCognitionErrorCodes;
import com.metaforge.common.exception.BizException;

public class InvalidFormatException extends BizException {

    public InvalidFormatException(String format) {
        super(AgentCognitionErrorCodes.INVALID_FORMAT,
                "输出格式 " + format + " 不受支持");
    }
}
