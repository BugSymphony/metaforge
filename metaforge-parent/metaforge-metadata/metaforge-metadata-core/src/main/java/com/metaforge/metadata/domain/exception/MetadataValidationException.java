package com.metaforge.metadata.domain.exception;

import com.metaforge.common.exception.BizException;
import static com.metaforge.metadata.api.constants.MetadataErrorCodes.SCHEMA_VALIDATION_FAILED;

public class MetadataValidationException extends BizException {
    public MetadataValidationException(String message) {
        super(SCHEMA_VALIDATION_FAILED, message);
    }
    public MetadataValidationException(String message, Object data) {
        super(SCHEMA_VALIDATION_FAILED, message, data);
    }
}
