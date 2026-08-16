package com.metaforge.agent.cognition.core.domain.exception;

import com.metaforge.agent.cognition.api.constants.AgentCognitionErrorCodes;
import com.metaforge.common.exception.BizException;

public class TemplateNotFoundException extends BizException {

    public TemplateNotFoundException(String templateId) {
        super(AgentCognitionErrorCodes.TEMPLATE_NOT_FOUND,
                "模板未注册: " + templateId);
    }

    public TemplateNotFoundException(String templateId, String detail) {
        super(AgentCognitionErrorCodes.TEMPLATE_NOT_FOUND,
                "模板未注册: " + templateId + " - " + detail);
    }
}
