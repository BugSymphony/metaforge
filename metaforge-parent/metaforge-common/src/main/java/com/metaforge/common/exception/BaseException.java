package com.metaforge.common.exception;

/**
 * 异常基类，全平台所有自定义异常的抽象根类。
 * 继承自 {@link RuntimeException}，提供错误码（code）、错误消息（message）
 * 和调试详细信息（detail）三个维度的异常描述能力。
 *
 * <p>异常层次结构：
 * <pre>
 * RuntimeException (JDK)
 * └── BaseException (abstract)
 *     ├── SystemException   — 系统级异常（基础设施故障）
 *     │   ├── DatabaseException   — 数据库操作异常
 *     │   └── RemoteCallException — 远程调用异常
 *     └── BizException      — 业务异常基类（供 BC 扩展）
 *         ├── ValidationException — 参数校验异常
 *         └── [BC 自定义扩展]
 * </pre>
 *
 * <p>错误码分配规则：
 * <ul>
 *   <li>10000-19999：系统级错误</li>
 *   <li>20000-29999：参数校验错误</li>
 *   <li>30000-49999：预留（业务 BC 使用）</li>
 *   <li>50000-59999：第三方服务错误</li>
 * </ul>
 */
public abstract class BaseException extends RuntimeException {

    /**
     * 错误码（5 位数字），用于标识异常类型。
     */
    private final int code;

    /**
     * 错误消息，可国际化。
     */
    private final String message;

    /**
     * 详细信息，用于开发调试，可选。
     */
    private final String detail;

    /**
     * 附加数据载荷，用于携带部分结果等业务数据，可选。
     */
    private final Object data;

    /**
     * 构造一个包含错误码和错误消息的异常实例。
     *
     * @param code    错误码
     * @param message 错误消息
     */
    public BaseException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
        this.detail = null;
        this.data = null;
    }

    /**
     * 构造一个包含错误码、错误消息、附加数据载荷的异常实例。
     *
     * @param code    错误码
     * @param message 错误消息
     * @param data    附加数据载荷（可为 null）
     */
    public BaseException(int code, String message, Object data) {
        super(message);
        this.code = code;
        this.message = message;
        this.detail = null;
        this.data = data;
    }

    /**
     * 构造一个包含错误码、错误消息和原始异常的异常实例。
     *
     * @param code    错误码
     * @param message 错误消息
     * @param cause   原始异常
     */
    public BaseException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
        this.detail = null;
        this.data = null;
    }

    /**
     * 构造一个包含错误码、错误消息、详细信息和原始异常的异常实例。
     * 此构造方法为受保护访问级别，仅供子类调用。
     *
     * @param code    错误码
     * @param message 错误消息
     * @param detail  详细信息（用于开发调试）
     * @param cause   原始异常
     */
    protected BaseException(int code, String message, String detail, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
        this.detail = detail;
        this.data = null;
    }

    /**
     * 获取错误码。
     *
     * @return 错误码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取错误消息。
     *
     * @return 错误消息
     */
    public String getMessage() {
        return message;
    }

    /**
     * 获取详细信息。
     *
     * @return 详细信息，可能为 null
     */
    public String getDetail() {
        return detail;
    }

    /**
     * 获取附加数据载荷。
     *
     * @return 附加数据载荷，可能为 null
     */
    public Object getData() {
        return data;
    }
}
