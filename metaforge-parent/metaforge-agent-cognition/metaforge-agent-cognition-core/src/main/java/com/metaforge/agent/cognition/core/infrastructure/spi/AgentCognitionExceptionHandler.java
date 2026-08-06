package com.metaforge.agent.cognition.core.infrastructure.spi;

import com.metaforge.agent.cognition.api.constant.AgentCognitionErrorCodes;
import com.metaforge.common.dto.ApiResponse;
import com.metaforge.common.spi.ExceptionHandlerSpi;
import com.metaforge.agent.cognition.core.domain.exception.AgentCognitionBizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class AgentCognitionExceptionHandler implements ExceptionHandlerSpi {

    private static final Logger log = LoggerFactory.getLogger(AgentCognitionExceptionHandler.class);

    @Override
    public ApiResponse<?> handle(Exception e) {
        if (e instanceof AgentCognitionBizException ex) {
            log.warn("元认知指导层业务异常 [{}] code={} message={}",
                    ex.getErrorCodeName(), ex.getCode(), ex.getMessage());
            return ApiResponse.error(ex.getCode(), ex.getMessage());
        }
        return null;
    }
}
