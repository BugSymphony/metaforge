package com.metaforge.computeengine.domain.exception;

import com.metaforge.computeengine.api.constant.ComputeEngineErrorCodes;

/**
 * 结果数量超限异常。
 */
public class ResultCountExceededException extends ComputeEngineException {

    public ResultCountExceededException(int currentCount, int maxCount) {
        super(ComputeEngineErrorCodes.RESULT_COUNT_EXCEEDED,
                ComputeEngineErrorCodes.RESULT_COUNT_EXCEEDED_MSG
                        + "，当前数量: " + currentCount + "，上限: " + maxCount);
    }

    @Override
    public String getErrorCodeName() {
        return "RESULT_COUNT_EXCEEDED";
    }
}
