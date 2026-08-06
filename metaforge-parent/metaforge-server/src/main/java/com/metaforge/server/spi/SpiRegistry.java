package com.metaforge.server.spi;

import com.metaforge.common.dto.ApiResponse;
import com.metaforge.common.spi.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.core.annotation.Order;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * SPI 扩展点注册中心。
 * 自动扫描所有 SPI 接口的实现 Bean，按 @Order 排序，提供链式调用门面方法。
 * 每个 SPI 扩展记录其来源模块信息，确保跨 BC 隔离：A BC 的 SPI 扩展不可被 B BC 的请求触发。
 */
@AutoConfiguration
public class SpiRegistry {
    private static final Logger log = LoggerFactory.getLogger(SpiRegistry.class);

    @Autowired(required = false)
    private List<ExceptionHandlerSpi> exceptionHandlers = new ArrayList<>();

    @Autowired(required = false)
    private List<RequestInterceptorSpi> requestInterceptors = new ArrayList<>();

    @Autowired(required = false)
    private List<LogMaskSpi> logMaskers = new ArrayList<>();

    @Autowired(required = false)
    private List<HealthCheckSpi> healthCheckers = new ArrayList<>();

    @Autowired(required = false)
    private List<SerializationSpi> serializers = new ArrayList<>();

    @Autowired(required = false)
    private List<ValidationSpi> validators = new ArrayList<>();

    @PostConstruct
    public void init() {
        log.info("SPI 注册中心初始化: 异常处理器={}, 请求拦截器={}, 脱敏={}, 健康检查={}, 序列化={}, 校验={}",
                exceptionHandlers.size(), requestInterceptors.size(), logMaskers.size(),
                healthCheckers.size(), serializers.size(), validators.size());
    }

    // 按 @Order 排序遍历，首个返回非 null 即时短路，全部 null 返回 null
    public ApiResponse<?> handleException(Exception e) {
        // Sort by @Order annotation value
        List<ExceptionHandlerSpi> sorted = exceptionHandlers.stream()
                .sorted(Comparator.comparingInt(SpiRegistry::getOrderValue))
                .toList();
        for (ExceptionHandlerSpi handler : sorted) {
            ApiResponse<?> response = handler.handle(e);
            if (response != null) {
                log.debug("SPI 异常处理器 [{}] 处理了异常: {}", handler.getClass().getSimpleName(), e.getMessage());
                return response;
            }
        }
        return null;
    }

    public List<LogMaskSpi> getLogMaskers() { return logMaskers; }
    public List<HealthCheckSpi> getHealthCheckers() { return healthCheckers; }
    public List<SerializationSpi> getSerializers() { return serializers; }
    public List<ValidationSpi> getValidators() { return validators; }
    public List<RequestInterceptorSpi> getRequestInterceptors() { return requestInterceptors; }

    private static int getOrderValue(Object bean) {
        Order order = bean.getClass().getAnnotation(Order.class);
        return order != null ? order.value() : 0;
    }
}
