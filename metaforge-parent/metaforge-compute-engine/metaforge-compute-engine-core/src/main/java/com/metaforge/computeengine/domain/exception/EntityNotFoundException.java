package com.metaforge.computeengine.domain.exception;

import com.metaforge.computeengine.api.constant.ComputeEngineErrorCodes;

/**
 * 实体不存在异常。
 */
public class EntityNotFoundException extends ComputeEngineException {

    public EntityNotFoundException(String fqn) {
        super(ComputeEngineErrorCodes.ENTITY_NOT_FOUND,
                ComputeEngineErrorCodes.ENTITY_NOT_FOUND_MSG + ": " + fqn);
    }

    @Override
    public String getErrorCodeName() {
        return "ENTITY_NOT_FOUND";
    }
}
