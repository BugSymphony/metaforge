package com.metaforge.metadata.domain.exception;

import com.metaforge.common.exception.BizException;
import static com.metaforge.metadata.api.constants.MetadataErrorCodes.DRAFT_NOT_FOUND;

public class DraftNotFoundException extends BizException {
    public DraftNotFoundException(String message) {
        super(DRAFT_NOT_FOUND, message);
    }
    public DraftNotFoundException(String message, Object data) {
        super(DRAFT_NOT_FOUND, message, data);
    }
}
