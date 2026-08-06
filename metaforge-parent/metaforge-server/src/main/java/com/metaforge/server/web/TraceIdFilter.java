package com.metaforge.server.web;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 全链路追踪过滤器
 * <p>为每个 HTTP 请求生成唯一的 traceId，写入 MDC 和响应头，
 * 方便日志追踪和问题排查。</p>
 */
@Component
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // 生成 32 位 UUID（去掉连字符），写入 MDC "traceId"
        // 设置响应头 X-Trace-Id
        // 请求结束后清理 MDC
        try {
            String traceId = UUID.randomUUID().toString().replace("-", "");
            MDC.put("traceId", traceId);
            response.setHeader("X-Trace-Id", traceId);
            chain.doFilter(request, response);
        } finally {
            MDC.remove("traceId");
        }
    }
}
