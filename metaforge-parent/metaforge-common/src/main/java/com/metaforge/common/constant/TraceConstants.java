package com.metaforge.common.constant;

/**
 * 全链路追踪相关常量定义。
 *
 * <p>TraceId 由服务端在请求入口（{@code TraceIdFilter}）生成，
 * 通过 HTTP 响应头 {@value #TRACE_ID_HEADER} 返回给客户端，
 * 同时在服务端通过 MDC Key {@value #TRACE_ID_MDC_KEY} 写入日志上下文，
 * 确保同一请求的所有日志行携带相同的 TraceId，便于日志聚合与问题排查。
 *
 * <p>本类仅供常量引用使用，不可实例化。
 */
public final class TraceConstants {

    private TraceConstants() {
        throw new UnsupportedOperationException("常量类不可实例化");
    }

    /**
     * HTTP 响应头名称，用于返回 TraceId 给客户端。
     */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    /**
     * SLF4J MDC 上下文 Key，用于在日志中注入 TraceId。
     */
    public static final String TRACE_ID_MDC_KEY = "traceId";
}
