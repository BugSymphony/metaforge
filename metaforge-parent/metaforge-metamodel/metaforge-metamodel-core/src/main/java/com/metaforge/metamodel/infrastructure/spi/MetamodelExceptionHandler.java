package com.metaforge.metamodel.infrastructure.spi;

import com.metaforge.common.dto.ApiResponse;
import com.metaforge.common.spi.ExceptionHandlerSpi;
import com.metaforge.metamodel.domain.exception.BaseMetamodelException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 元模型 BC 统一异常处理 SPI 实现。
 * 将 BaseMetamodelException 子类映射为 ApiResponse.error()。
 */
@Component
public class MetamodelExceptionHandler implements ExceptionHandlerSpi {

    private static final Logger log = LoggerFactory.getLogger(MetamodelExceptionHandler.class);

    @Override
    public ApiResponse<?> handle(Exception e) {
        if (e instanceof BaseMetamodelException ex) {
            log.warn("元模型业务异常 [{}] code={} message={}",
                    ex.getErrorCodeName(), ex.getCode(), ex.getMessage());
            return ApiResponse.error(ex.getCode(), ex.getMessage());
        }
        return null;
    }
}
