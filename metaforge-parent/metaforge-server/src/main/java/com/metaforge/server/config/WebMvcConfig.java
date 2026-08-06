package com.metaforge.server.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.metaforge.server.web.TraceIdFilter;

/**
 * Web MVC 配置
 * <p>注册 TraceIdFilter 过滤器，并配置全局 CORS 跨域策略。</p>
 */
@AutoConfiguration(after = {VirtualThreadConfig.class, JacksonConfig.class})
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 注册 TraceIdFilter 过滤器
     * <p>设置最高优先级，拦截所有请求路径，确保每个请求都有全链路追踪 ID。</p>
     *
     * @param filter TraceIdFilter 实例
     * @return FilterRegistrationBean 过滤器注册 Bean
     */
    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration(TraceIdFilter filter) {
        FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    /**
     * 配置全局 CORS 跨域策略
     * <p>允许所有来源、所有请求头、常用 HTTP 方法，并支持携带凭证。</p>
     *
     * @param registry CORS 注册表
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
