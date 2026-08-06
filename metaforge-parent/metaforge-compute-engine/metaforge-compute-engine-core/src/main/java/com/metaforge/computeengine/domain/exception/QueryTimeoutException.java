package com.metaforge.computeengine.domain.exception;

import com.metaforge.computeengine.api.constant.ComputeEngineErrorCodes;

/**
 * 查询超时异常。
 */
public class QueryTimeoutException extends ComputeEngineException {

    public QueryTimeoutException(long elapsedMs, long timeoutMs) {
        super(ComputeEngineErrorCodes.QUERY_TIMEOUT,
                ComputeEngineErrorCodes.QUERY_TIMEOUT_MSG
                        + "，已耗时: " + elapsedMs + "ms，超时阈值: " + timeoutMs + "ms");
    }

    @Override
    public String getErrorCodeName() {
        return "QUERY_TIMEOUT";
    }
}
