package com.metaforge.agent.cognition.core.infrastructure.exception;

import com.metaforge.agent.cognition.api.exception.InvalidLevelException;
import com.metaforge.agent.cognition.api.exception.InvalidOperatorSelectionException;
import com.metaforge.agent.cognition.core.domain.exception.*;
import com.metaforge.common.dto.ApiResponse;
import com.metaforge.common.spi.ExceptionHandlerSpi;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(100)
public class AgentCognitionExceptionHandler implements ExceptionHandlerSpi {

    @Override
    public ApiResponse<?> handle(Exception e) {
        if (e instanceof TemplateNotFoundException te) {
            return ApiResponse.error(te.getCode(), te.getMessage());
        }
        if (e instanceof InvalidScopeException se) {
            return ApiResponse.error(se.getCode(), se.getMessage());
        }
        if (e instanceof MissingScopeException mse) {
            return ApiResponse.error(mse.getCode(), mse.getMessage());
        }
        if (e instanceof EntityOutOfScopeException eose) {
            return ApiResponse.error(eose.getCode(), eose.getMessage());
        }
        if (e instanceof ArchetypeNotSupportedException anse) {
            return ApiResponse.error(anse.getCode(), anse.getMessage());
        }
        if (e instanceof OperatorExecutionException oee) {
            return ApiResponse.error(oee.getCode(), oee.getMessage());
        }
        if (e instanceof OperatorTimeoutException ote) {
            return ApiResponse.error(ote.getCode(), ote.getMessage());
        }
        if (e instanceof InvalidFormatException ife) {
            return ApiResponse.error(ife.getCode(), ife.getMessage());
        }
        if (e instanceof UpstreamUnavailableException uue) {
            return ApiResponse.error(uue.getCode(), uue.getMessage());
        }
        if (e instanceof TemplateInvalidException tie) {
            return ApiResponse.error(tie.getCode(), tie.getMessage());
        }
        if (e instanceof UnsupportedOperatorException uoe) {
            return ApiResponse.error(uoe.getCode(), uoe.getMessage());
        }
        if (e instanceof UnknownOperatorRefException uore) {
            return ApiResponse.error(uore.getCode(), uore.getMessage());
        }
        if (e instanceof InvalidLevelException ile) {
            return ApiResponse.error(ile.getCode(), ile.getMessage());
        }
        if (e instanceof InvalidOperatorSelectionException iose) {
            return ApiResponse.error(iose.getCode(), iose.getMessage());
        }
        return null;
    }
}
