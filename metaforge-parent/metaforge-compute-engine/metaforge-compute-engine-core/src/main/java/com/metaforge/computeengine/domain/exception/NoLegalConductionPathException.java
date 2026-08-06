package com.metaforge.computeengine.domain.exception;

import com.metaforge.computeengine.api.constant.ComputeEngineErrorCodes;

/**
 * 无合法传导路径异常。
 */
public class NoLegalConductionPathException extends ComputeEngineException {

    public NoLegalConductionPathException(String sourceFqn, String targetFqn) {
        super(ComputeEngineErrorCodes.NO_LEGAL_CONDUCTION_PATH,
                ComputeEngineErrorCodes.NO_LEGAL_CONDUCTION_PATH_MSG
                        + "，起点: " + sourceFqn + "，终点: " + targetFqn);
    }

    @Override
    public String getErrorCodeName() {
        return "NO_LEGAL_CONDUCTION_PATH";
    }
}
