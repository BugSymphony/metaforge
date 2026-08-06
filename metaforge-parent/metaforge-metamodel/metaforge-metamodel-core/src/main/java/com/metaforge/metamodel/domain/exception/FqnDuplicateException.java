package com.metaforge.metamodel.domain.exception;

import static com.metaforge.metamodel.api.constants.ErrorCodes.FQN_DUPLICATE;

/**
 * FQN 全局重复异常。
 */
public class FqnDuplicateException extends BaseMetamodelException {

    public FqnDuplicateException(String fqn) {
        super(FQN_DUPLICATE, "FQN 已存在: " + fqn);
    }

    public FqnDuplicateException(String fqn, Throwable cause) {
        super(FQN_DUPLICATE, "FQN 已存在: " + fqn, cause);
    }

    @Override
    public String getErrorCodeName() {
        return "FQN_DUPLICATE";
    }
}
