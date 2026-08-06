package com.metaforge.common.dto;

import java.io.Serial;
import java.io.Serializable;

import org.slf4j.MDC;

/**
 * 全平台统一响应体，所有 REST 接口均使用此类封装返回值。
 *
 * <p>包含状态码、提示消息、响应数据体以及全链路追踪标识。
 * 通过静态工厂方法创建实例，traceId 自动从 MDC 中获取。
 *
 * @param <T> 响应数据体类型
 */
public final class ApiResponse<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 业务状态码，200 表示成功 */
    private int code;

    /** 提示信息，成功时为 "success" */
    private String message;

    /** 响应数据体，错误时为 null */
    private T data;

    /** 全链路追踪标识，由 TraceIdFilter 自动注入 MDC */
    private String traceId;

    /**
     * 私有默认构造方法，强制使用静态工厂方法创建实例。
     */
    private ApiResponse() {
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 创建成功响应（默认消息）。
     *
     * @param data 响应数据体
     * @param <T>  数据体类型
     * @return 成功响应实例，code=200，message="success"
     */
    public static <T> ApiResponse<T> success(T data) {
        return success(data, "success");
    }

    /**
     * 创建成功响应（自定义消息）。
     *
     * @param data    响应数据体
     * @param message 自定义提示消息
     * @param <T>     数据体类型
     * @return 成功响应实例，code=200
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return of(200, message, data);
    }

    /**
     * 创建错误响应，data 为 null。
     *
     * @param code    错误码
     * @param message 错误消息
     * @param <T>     数据体类型
     * @return 错误响应实例
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return of(code, message, null);
    }

    /**
     * 创建自定义响应，全部字段由调用方指定。
     *
     * @param code    状态码
     * @param message 提示消息
     * @param data    响应数据体
     * @param <T>     数据体类型
     * @return 完整响应实例
     */
    public static <T> ApiResponse<T> of(int code, String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.code = code;
        response.message = message;
        response.data = data;
        response.traceId = getTraceIdFromMdc();
        return response;
    }

    // ==================== Getters & Setters ====================

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    // ==================== 内部工具方法 ====================

    /**
     * 从 MDC 中获取 traceId，若不存在则返回空字符串。
     */
    private static String getTraceIdFromMdc() {
        String traceId = MDC.get("traceId");
        return traceId != null ? traceId : "";
    }
}
