package com.metaforge.computeengine.domain.exception;

import com.metaforge.computeengine.api.constant.ComputeEngineErrorCodes;

/**
 * 批量查询 FQN 数量超限异常。
 */
public class BatchSizeExceededException extends ComputeEngineException {

    public BatchSizeExceededException(int requested, int max) {
        super(ComputeEngineErrorCodes.BATCH_SIZE_EXCEEDED,
                ComputeEngineErrorCodes.BATCH_SIZE_EXCEEDED_MSG
                        + "，请求数量: " + requested + "，上限: " + max);
    }

    @Override
    public String getErrorCodeName() {
        return "BATCH_SIZE_EXCEEDED";
    }
}
