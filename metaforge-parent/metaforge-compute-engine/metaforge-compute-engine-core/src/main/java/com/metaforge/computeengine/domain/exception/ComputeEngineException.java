package com.metaforge.computeengine.domain.exception;

import com.metaforge.common.exception.BizException;

/**
 * 语义查询与推理引擎统一业务异常基类。
 *
 * <p>所有 BC 内自定义异常均继承此类，通过 errorCode 关联 {@link com.metaforge.computeengine.api.constant.ComputeEngineErrorCodes} 常量。
 * 错误码范围 33000-33999。
 *
 * @author metaforge
 */
public abstract class ComputeEngineException extends BizException {

    protected ComputeEngineException(int code, String message) {
        super(code, message);
    }

    protected ComputeEngineException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }

    /**
     * 获取异常对应的错误码名称，用于统一异常处理映射与日志记录。
     *
     * @return 错误码常量名称
     */
    public abstract String getErrorCodeName();
}
