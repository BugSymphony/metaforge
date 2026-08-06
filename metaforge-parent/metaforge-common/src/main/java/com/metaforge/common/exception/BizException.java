package com.metaforge.common.exception;

import com.metaforge.common.constant.ErrorCodes;

/**
 * 业务异常基类，供各个业务 BC 扩展使用。
 * 预留错误码范围：30000-49999，各 BC 可在此范围内自主分配错误码。
 *
 * <p>子类：
 * <ul>
 *   <li>{@link ValidationException} — 参数校验异常，错误码 20002</li>
 *   <li>[BC 自定义扩展] — 各业务 BC 通过继承本类定义自身业务异常</li>
 * </ul>
 */
public class BizException extends BaseException {

    /**
     * 使用指定的错误码和错误消息构造业务异常实例。
     *
     * @param code    错误码（建议在 30000-49999 范围内）
     * @param message 错误消息
     */
    public BizException(int code, String message) {
        super(code, message);
    }

    /**
     * 使用指定的错误码、错误消息和原始异常构造业务异常实例。
     *
     * @param code    错误码（建议在 30000-49999 范围内）
     * @param message 错误消息
     * @param cause   原始异常
     */
    public BizException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }

    /**
     * 使用指定的错误码、错误消息和附加数据载荷构造业务异常实例。
     *
     * @param code    错误码（建议在 30000-49999 范围内）
     * @param message 错误消息
     * @param data    附加数据载荷（可为 null）
     */
    public BizException(int code, String message, Object data) {
        super(code, message, data);
    }

    /**
     * 参数校验异常。
     * 错误码固定为 {@link ErrorCodes#VALIDATION_FORMAT_ERROR}（20002）。
     */
    public static class ValidationException extends BizException {

        /**
         * 使用指定的错误消息构造参数校验异常实例。
         *
         * @param message 错误消息（校验失败描述）
         */
        public ValidationException(String message) {
            super(ErrorCodes.VALIDATION_FORMAT_ERROR, message);
        }

        /**
         * 使用指定的错误消息和原始异常构造参数校验异常实例。
         *
         * @param message 错误消息（校验失败描述）
         * @param cause   原始异常（如 {@link jakarta.validation.ConstraintViolationException}）
         */
        public ValidationException(String message, Throwable cause) {
            super(ErrorCodes.VALIDATION_FORMAT_ERROR, message, cause);
        }
    }
}
