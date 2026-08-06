package com.metaforge.computeengine.domain.exception;

import com.metaforge.computeengine.api.constant.ComputeEngineErrorCodes;

/**
 * 遍历深度超限异常。
 */
public class TraversalDepthExceededException extends ComputeEngineException {

    public TraversalDepthExceededException(int currentDepth, int maxDepth) {
        super(ComputeEngineErrorCodes.TRAVERSAL_DEPTH_EXCEEDED,
                ComputeEngineErrorCodes.TRAVERSAL_DEPTH_EXCEEDED_MSG
                        + "，当前深度: " + currentDepth + "，上限: " + maxDepth);
    }

    @Override
    public String getErrorCodeName() {
        return "TRAVERSAL_DEPTH_EXCEEDED";
    }
}
