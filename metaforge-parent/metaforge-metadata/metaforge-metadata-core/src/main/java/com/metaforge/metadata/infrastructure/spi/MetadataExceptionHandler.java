package com.metaforge.metadata.infrastructure.spi;

import com.metaforge.common.dto.ApiResponse;
import com.metaforge.common.spi.ExceptionHandlerSpi;
import com.metaforge.metadata.domain.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MetadataExceptionHandler implements ExceptionHandlerSpi {

    private static final Logger log = LoggerFactory.getLogger(MetadataExceptionHandler.class);

    @Override
    public ApiResponse<?> handle(Exception e) {
        if (e instanceof MetadataValidationException ex) {
            log.warn("元数据校验异常 code={} message={}", ex.getCode(), ex.getMessage());
            return ApiResponse.error(ex.getCode(), ex.getMessage());
        }
        if (e instanceof FqnConflictException ex) {
            log.warn("FQN 冲突异常 code={} message={}", ex.getCode(), ex.getMessage());
            return ApiResponse.error(ex.getCode(), ex.getMessage());
        }
        if (e instanceof EntityNotFoundException ex) {
            log.warn("实体未找到异常 code={} message={}", ex.getCode(), ex.getMessage());
            return ApiResponse.error(ex.getCode(), ex.getMessage());
        }
        if (e instanceof DraftNotFoundException ex) {
            log.warn("草稿未找到异常 code={} message={}", ex.getCode(), ex.getMessage());
            return ApiResponse.error(ex.getCode(), ex.getMessage());
        }
        if (e instanceof ActivationFailedException ex) {
            log.warn("生效失败异常 code={} message={}", ex.getCode(), ex.getMessage());
            return ApiResponse.error(ex.getCode(), ex.getMessage());
        }
        if (e instanceof DeactivationBlockedException ex) {
            log.warn("下线被拦截异常 code={} message={}", ex.getCode(), ex.getMessage());
            return ApiResponse.error(ex.getCode(), ex.getMessage());
        }
        return null;
    }
}
