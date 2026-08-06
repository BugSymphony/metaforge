package com.metaforge.common.exception;

import com.metaforge.common.constant.ErrorCodes;

/**
 * 系统级异常，表示基础设施层面的故障（如数据库连接失败、远程调用超时等）。
 * 错误码范围：10000-19999。
 *
 * <p>子类：
 * <ul>
 *   <li>{@link DatabaseException}   — 数据库操作异常，错误码 10002</li>
 *   <li>{@link RemoteCallException} — 远程调用异常，错误码 10003</li>
 * </ul>
 */
public class SystemException extends BaseException {

    /**
     * 默认访问级别的构造方法，创建包含错误码、错误消息和详细信息的系统异常。
     *
     * @param code    错误码
     * @param message 错误消息
     * @param detail  详细信息
     */
    SystemException(int code, String message, String detail) {
        super(code, message, detail, null);
    }

    /**
     * 静态工厂方法，创建包含错误码、错误消息和详细信息的系统异常实例。
     *
     * @param code    错误码
     * @param message 错误消息
     * @param detail  详细信息
     * @return 系统异常实例
     */
    public static SystemException of(int code, String message, String detail) {
        return new SystemException(code, message, detail);
    }

    /**
     * 数据库操作异常。
     * 错误码固定为 {@link ErrorCodes#DATABASE_ERROR}（10002）。
     */
    public static class DatabaseException extends SystemException {

        /**
         * 使用指定的详细信息构造数据库异常实例。
         *
         * @param detail 详细信息（用于开发调试）
         */
        public DatabaseException(String detail) {
            super(ErrorCodes.DATABASE_ERROR, "数据库操作异常", detail);
        }
    }

    /**
     * 远程调用异常。
     * 错误码固定为 {@link ErrorCodes#REMOTE_CALL_ERROR}（10003）。
     */
    public static class RemoteCallException extends SystemException {

        /**
         * 使用指定的详细信息构造远程调用异常实例。
         *
         * @param detail 详细信息（用于开发调试）
         */
        public RemoteCallException(String detail) {
            super(ErrorCodes.REMOTE_CALL_ERROR, "远程调用异常", detail);
        }
    }
}
