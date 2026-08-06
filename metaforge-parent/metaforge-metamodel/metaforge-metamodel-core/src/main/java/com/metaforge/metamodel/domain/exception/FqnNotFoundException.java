package com.metaforge.metamodel.domain.exception;

import static com.metaforge.metamodel.api.constants.ErrorCodes.FQN_NOT_FOUND;

/**
 * FQN 引用目标不存在异常。
 */
public class FqnNotFoundException extends BaseMetamodelException {

    public FqnNotFoundException(String fqn) {
        super(FQN_NOT_FOUND, "FQN 目标不存在: " + fqn);
    }

    public FqnNotFoundException(String fqn, Throwable cause) {
        super(FQN_NOT_FOUND, "FQN 目标不存在: " + fqn, cause);
    }

    @Override
    public String getErrorCodeName() {
        return "FQN_NOT_FOUND";
    }
}
