package com.metaforge.computeengine.domain.exception;

import com.metaforge.computeengine.api.constant.ComputeEngineErrorCodes;

/**
 * 图模式匹配语法非法异常。
 */
public class InvalidPatternException extends ComputeEngineException {

    public InvalidPatternException(String pattern) {
        super(ComputeEngineErrorCodes.INVALID_PATTERN,
                ComputeEngineErrorCodes.INVALID_PATTERN_MSG + ": " + pattern);
    }

    public InvalidPatternException(String pattern, String detail) {
        super(ComputeEngineErrorCodes.INVALID_PATTERN,
                ComputeEngineErrorCodes.INVALID_PATTERN_MSG + " [" + pattern + "]: " + detail);
    }

    @Override
    public String getErrorCodeName() {
        return "INVALID_PATTERN";
    }
}
