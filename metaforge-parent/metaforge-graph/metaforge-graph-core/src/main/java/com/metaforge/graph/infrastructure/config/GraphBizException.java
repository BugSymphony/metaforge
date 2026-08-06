package com.metaforge.graph.infrastructure.config;

import com.metaforge.common.exception.BizException;

/**
 * metaforge-graph BC 统一业务异常基类。
 * 所有 BC 内自定义异常均继承此类，通过 errorCode 关联 GraphErrorCode 常量。
 */
public abstract class GraphBizException extends BizException {

    protected GraphBizException(int code, String message) {
        super(code, message);
    }

    protected GraphBizException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }

    protected GraphBizException(int code, String message, Object data) {
        super(code, message, data);
    }

    /**
     * 获取异常对应的错误码名称，用于统一异常处理映射与日志记录。
     */
    public abstract String getErrorCodeName();
}
