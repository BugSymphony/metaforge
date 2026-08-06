package com.metaforge.common.constant;

/**
 * 全平台统一错误码常量定义。
 *
 * <p>错误码分配规则：
 * <ul>
 *   <li>10000-19999：系统级错误</li>
 *   <li>20000-29999：参数校验错误</li>
 *   <li>30000-49999：预留（业务 BC 使用，各 BC 自定义分配）</li>
 *   <li>50000-59999：第三方服务错误</li>
 * </ul>
 *
 * <p>本类仅供常量引用使用，不可实例化。
 */
public final class ErrorCodes {

    private ErrorCodes() {
        throw new UnsupportedOperationException("常量类不可实例化");
    }

    // ========== 系统级错误（10000-19999） ==========

    /** 通用系统错误 */
    public static final int SYSTEM_ERROR = 10000;

    /** 系统内部错误 */
    public static final int SYSTEM_INTERNAL_ERROR = 10001;

    /** 数据库操作异常 */
    public static final int DATABASE_ERROR = 10002;

    /** 远程调用异常 */
    public static final int REMOTE_CALL_ERROR = 10003;

    // ========== 参数校验错误（20000-29999） ==========

    /** 参数校验失败 */
    public static final int VALIDATION_ERROR = 20001;

    /** 参数格式错误 */
    public static final int VALIDATION_FORMAT_ERROR = 20002;

    /** 资源不存在 */
    public static final int RESOURCE_NOT_FOUND = 20404;
}
