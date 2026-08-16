package com.metaforge.agent.cognition.core.domain.exception;

import com.metaforge.agent.cognition.api.constants.AgentCognitionErrorCodes;
import com.metaforge.common.exception.BizException;

public class EntityOutOfScopeException extends BizException {

    public EntityOutOfScopeException(String entityFqn, String scopeDescription) {
        super(AgentCognitionErrorCodes.ENTITY_OUT_OF_SCOPE,
                "entityFqn " + entityFqn + " 不在 scope 范围内: " + scopeDescription);
    }
}
