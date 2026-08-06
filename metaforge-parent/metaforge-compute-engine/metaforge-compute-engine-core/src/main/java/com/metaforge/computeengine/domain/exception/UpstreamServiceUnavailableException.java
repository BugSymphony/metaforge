package com.metaforge.computeengine.domain.exception;

import com.metaforge.computeengine.api.constant.ComputeEngineErrorCodes;

/**
 * 上游 BC 模块不可用异常。
 */
public class UpstreamServiceUnavailableException extends ComputeEngineException {

    public UpstreamServiceUnavailableException(String bcName) {
        super(ComputeEngineErrorCodes.UPSTREAM_SERVICE_UNAVAILABLE,
                ComputeEngineErrorCodes.UPSTREAM_SERVICE_UNAVAILABLE_MSG + " [" + bcName + "]");
    }

    public UpstreamServiceUnavailableException(String bcName, Throwable cause) {
        super(ComputeEngineErrorCodes.UPSTREAM_SERVICE_UNAVAILABLE,
                ComputeEngineErrorCodes.UPSTREAM_SERVICE_UNAVAILABLE_MSG + " [" + bcName + "]", cause);
    }

    @Override
    public String getErrorCodeName() {
        return "UPSTREAM_SERVICE_UNAVAILABLE";
    }
}
