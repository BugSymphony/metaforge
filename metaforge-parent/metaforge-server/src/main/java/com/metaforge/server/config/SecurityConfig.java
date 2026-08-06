package com.metaforge.server.config;

import java.io.IOException;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequestWrapper;

/**
 * 安全基线配置
 * <p>注册 XSS 过滤器，对请求参数中的脚本标签进行清洗，防止跨站脚本攻击。</p>
 */
@AutoConfiguration
public class SecurityConfig {

    /**
     * 注册 XSS 过滤器
     *
     * @return FilterRegistrationBean 实例
     */
    @Bean
    public FilterRegistrationBean<SimpleXssFilter> xssFilterRegistration() {
        FilterRegistrationBean<SimpleXssFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SimpleXssFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Integer.MIN_VALUE);
        return registration;
    }

    /**
     * 简单 XSS 过滤器
     * <p>拦截所有请求，对请求参数中的 {@code <script>} 标签进行清洗。</p>
     */
    static class SimpleXssFilter extends HttpFilter {

        private static final long serialVersionUID = 1L;

        @Override
        protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            chain.doFilter(new XssRequestWrapper(request), response);
        }
    }

    /**
     * XSS 请求包装器
     * <p>重写 getParameter 相关方法，对参数值中的脚本标签进行清理。</p>
     */
    static class XssRequestWrapper extends HttpServletRequestWrapper {

        XssRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            return sanitize(value);
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) {
                return null;
            }
            String[] sanitized = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                sanitized[i] = sanitize(values[i]);
            }
            return sanitized;
        }

        @Override
        public java.util.Map<String, String[]> getParameterMap() {
            java.util.Map<String, String[]> map = super.getParameterMap();
            java.util.Map<String, String[]> sanitized = new java.util.LinkedHashMap<>();
            for (java.util.Map.Entry<String, String[]> entry : map.entrySet()) {
                String[] values = entry.getValue();
                String[] cleaned = new String[values.length];
                for (int i = 0; i < values.length; i++) {
                    cleaned[i] = sanitize(values[i]);
                }
                sanitized.put(entry.getKey(), cleaned);
            }
            return sanitized;
        }

        /**
         * 清洗参数值，移除脚本标签
         *
         * @param value 原始参数值
         * @return 清洗后的值
         */
        private String sanitize(String value) {
            if (value == null) {
                return null;
            }
            return value.replaceAll("(?i)<script.*?>.*?</script>", "")
                    .replaceAll("(?i)<script.*?/>", "");
        }
    }
}
