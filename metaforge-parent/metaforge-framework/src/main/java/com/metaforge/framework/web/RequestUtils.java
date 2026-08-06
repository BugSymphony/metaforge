package com.metaforge.framework.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.slf4j.MDC;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Web 请求工具类，提供客户端 IP、TraceId、请求体读取等常用方法。
 */
public final class RequestUtils {

    private static final String TRACE_ID_KEY = "traceId";

    /** 未知 IP 占位符 */
    private static final String UNKNOWN = "unknown";

    private RequestUtils() {
    }

    /**
     * 获取客户端真实 IP，处理代理和负载均衡场景。
     * 优先级：X-Forwarded-For → Proxy-Client-IP → WL-Proxy-Client-IP → getRemoteAddr
     *
     * @param request HTTP 请求
     * @return 客户端真实 IP
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // X-Forwarded-For 可能包含多个 IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    /**
     * 获取当前请求的 TraceId。
     * 优先从 MDC 中获取，fallback 返回空字符串。
     *
     * @return TraceId 或空字符串
     */
    public static String getTraceId() {
        String traceId = MDC.get(TRACE_ID_KEY);
        return traceId != null ? traceId : "";
    }

    /**
     * 读取请求体内容（仅用于日志等非业务场景，不会消费输入流）。
     * 方法内部使用 {@code request.getReader()} 读取，注意：
     * 调用后会消费输入流，业务代码不可再次读取。
     *
     * @param request HTTP 请求
     * @return 请求体字符串；读取失败时返回空字符串
     */
    public static String getRequestBody(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            // 读取失败时返回空字符串
            return "";
        }
        return sb.toString();
    }
}
