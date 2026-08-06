package com.metaforge.metadata.domain.exception;

import com.metaforge.common.exception.BizException;
import static com.metaforge.metadata.api.constants.MetadataErrorCodes.FQN_CONFLICT;

public class FqnConflictException extends BizException {
    public FqnConflictException(String message) {
        super(FQN_CONFLICT, message);
    }
    public FqnConflictException(String message, Object data) {
        super(FQN_CONFLICT, message, data);
    }
}
