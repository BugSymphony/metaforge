package com.metaforge.server.web;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.metaforge.common.dto.ApiResponse;

/**
 * 全局响应体统一包装
 * <p>对 com.metaforge 包下所有控制器的返回值进行统一包装，
 * 将原始返回值自动包装为 {@link ApiResponse} 格式。</p>
 */
@RestControllerAdvice(basePackages = "com.metaforge")
public class GlobalResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        Class<?> type = returnType.getParameterType();
        // ResponseEntity / HttpEntity 已携带完整状态与响应体，不再包装
        if (HttpEntity.class.isAssignableFrom(type) || ResponseEntity.class.isAssignableFrom(type)) {
            return false;
        }
        // 如果返回值已经是 ApiResponse 类型，不再包装
        return !ApiResponse.class.isAssignableFrom(type);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType contentType,
                                  Class converterType, ServerHttpRequest request, ServerHttpResponse response) {
        // 将原始返回值包装为 ApiResponse.success
        return ApiResponse.success(body);
    }
}
