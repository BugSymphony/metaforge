package com.metaforge.metadata.domain.exception;

import com.metaforge.common.exception.BizException;
import static com.metaforge.metadata.api.constants.MetadataErrorCodes.ACTIVATION_FAILED;

public class ActivationFailedException extends BizException {
    public ActivationFailedException(String message) {
        super(ACTIVATION_FAILED, message);
    }
    public ActivationFailedException(String message, Object data) {
        super(ACTIVATION_FAILED, message, data);
    }
}
