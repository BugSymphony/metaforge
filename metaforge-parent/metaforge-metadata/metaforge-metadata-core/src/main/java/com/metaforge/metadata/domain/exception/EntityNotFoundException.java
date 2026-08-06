package com.metaforge.metadata.domain.exception;

import com.metaforge.common.exception.BizException;
import static com.metaforge.metadata.api.constants.MetadataErrorCodes.ENTITY_NOT_FOUND;

public class EntityNotFoundException extends BizException {
    public EntityNotFoundException(String message) {
        super(ENTITY_NOT_FOUND, message);
    }
    public EntityNotFoundException(String message, Object data) {
        super(ENTITY_NOT_FOUND, message, data);
    }
}
