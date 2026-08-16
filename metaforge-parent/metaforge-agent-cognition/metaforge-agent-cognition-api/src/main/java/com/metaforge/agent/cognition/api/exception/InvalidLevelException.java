package com.metaforge.agent.cognition.api.exception;

import com.metaforge.agent.cognition.api.constants.AgentCognitionErrorCodes;
import com.metaforge.common.exception.BizException;

public class InvalidLevelException extends BizException {

    public InvalidLevelException(String level, String templateId) {
        super(AgentCognitionErrorCodes.INVALID_LEVEL,
                "level " + level + " 无法解析为有效 EntitySchema 类型（模板 " + templateId + "）");
    }
}
