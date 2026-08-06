package com.metaforge.computeengine.domain.exception;

import com.metaforge.computeengine.api.constant.ComputeEngineErrorCodes;

/**
 * 循环引用异常。
 */
public class CircularReferenceException extends ComputeEngineException {

    public CircularReferenceException(String fqn) {
        super(ComputeEngineErrorCodes.CIRCULAR_REFERENCE_DETECTED,
                ComputeEngineErrorCodes.CIRCULAR_REFERENCE_DETECTED_MSG + "，实体 FQN: " + fqn);
    }

    @Override
    public String getErrorCodeName() {
        return "CIRCULAR_REFERENCE_DETECTED";
    }
}
