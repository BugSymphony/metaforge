package com.metaforge.agent.cognition.core.domain.exception;

import com.metaforge.agent.cognition.api.constants.AgentCognitionErrorCodes;
import com.metaforge.common.exception.BizException;

public class UpstreamUnavailableException extends BizException {

    public UpstreamUnavailableException(String bcName, String detail) {
        super(AgentCognitionErrorCodes.UPSTREAM_UNAVAILABLE,
                "上游 BC " + bcName + " 不可用: " + detail);
    }
}
