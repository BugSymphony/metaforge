package com.metaforge.graph.infrastructure.config;

import com.metaforge.common.dto.ApiResponse;
import com.metaforge.common.spi.ExceptionHandlerSpi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * metaforge-graph BC 统一异常处理 SPI 实现。
 * 将 GraphBizException 子类映射为 ApiResponse.error()。
 */
@Component
@Order(100)
public class GraphExceptionHandlerSpi implements ExceptionHandlerSpi {

    private static final Logger log = LoggerFactory.getLogger(GraphExceptionHandlerSpi.class);

    @Override
    public ApiResponse<?> handle(Exception e) {
        if (e instanceof GraphBizException ex) {
            log.warn("语义关系网络业务异常 [{}] code={} message={}",
                    ex.getErrorCodeName(), ex.getCode(), ex.getMessage());
            return ApiResponse.error(ex.getCode(), ex.getMessage());
        }
        return null;
    }
}
