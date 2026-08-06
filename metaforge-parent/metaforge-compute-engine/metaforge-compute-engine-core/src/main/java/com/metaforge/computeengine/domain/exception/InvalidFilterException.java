package com.metaforge.computeengine.domain.exception;

import com.metaforge.computeengine.api.constant.ComputeEngineErrorCodes;

/**
 * 过滤参数组合非法异常。
 */
public class InvalidFilterException extends ComputeEngineException {

    public InvalidFilterException(String detail) {
        super(ComputeEngineErrorCodes.INVALID_FILTER,
                ComputeEngineErrorCodes.INVALID_FILTER_MSG + ": " + detail);
    }

    @Override
    public String getErrorCodeName() {
        return "INVALID_FILTER";
    }
}
