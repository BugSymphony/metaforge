package com.metaforge.metamodel.domain.exception;

import com.metaforge.common.exception.BizException;

/**
 * 元模型治理 BC 统一业务异常基类。
 * 所有 BC 内自定义异常均继承此类，通过 errorCode 关联 ErrorCodes 常量。
 */
public abstract class BaseMetamodelException extends BizException {

    protected BaseMetamodelException(int code, String message) {
        super(code, message);
    }

    protected BaseMetamodelException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }

    protected BaseMetamodelException(int code, String message, Object data) {
        super(code, message, data);
    }

    /**
     * 获取异常对应的错误码枚举名称，用于统一异常处理映射。
     */
    public abstract String getErrorCodeName();
}
