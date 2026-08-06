package com.metaforge.sample.spi;

import com.metaforge.common.dto.ApiResponse;
import com.metaforge.common.spi.ExceptionHandlerSpi;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * bc-sample 自定义异常处理器。
 * 处理 IllegalArgumentException，返回业务错误码 30101。
 */
@Component
@Order(100)
public class SampleExceptionHandler implements ExceptionHandlerSpi {
    @Override
    public ApiResponse<?> handle(Exception e) {
        if (e instanceof IllegalArgumentException) {
            return ApiResponse.error(30101, "BC 自定义: " + e.getMessage());
        }
        return null;
    }
}
