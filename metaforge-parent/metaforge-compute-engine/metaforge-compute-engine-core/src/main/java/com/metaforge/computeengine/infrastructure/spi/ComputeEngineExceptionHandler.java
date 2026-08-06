package com.metaforge.computeengine.infrastructure.spi;

import com.metaforge.common.dto.ApiResponse;
import com.metaforge.common.spi.ExceptionHandlerSpi;
import com.metaforge.computeengine.domain.exception.ComputeEngineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 语义查询与推理引擎 BC 统一异常处理 SPI 实现。
 *
 * <p>将 ComputeEngineException 及其子类映射为 ApiResponse.error()，错误码范围 33000-33999。
 *
 * @author metaforge
 */
@Component
@Order(100)
public class ComputeEngineExceptionHandler implements ExceptionHandlerSpi {

    private static final Logger log = LoggerFactory.getLogger(ComputeEngineExceptionHandler.class);

    @Override
    public ApiResponse<?> handle(Exception e) {
        if (e instanceof ComputeEngineException ce) {
            log.warn("语义查询引擎业务异常 [{}] code={} message={}",
                    ce.getErrorCodeName(), ce.getCode(), ce.getMessage());
            return ApiResponse.error(ce.getCode(), ce.getMessage());
        }
        return null;
    }
}
