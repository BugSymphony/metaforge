package com.metaforge.metadata.domain.exception;

import com.metaforge.common.exception.BizException;
import static com.metaforge.metadata.api.constants.MetadataErrorCodes.DEACTIVATION_BLOCKED;

import java.util.List;

public class DeactivationBlockedException extends BizException {

    private final List<String> blockReasons;

    public DeactivationBlockedException(String message, List<String> blockReasons) {
        super(DEACTIVATION_BLOCKED, message);
        this.blockReasons = blockReasons;
    }

    public DeactivationBlockedException(List<String> blockReasons) {
        super(DEACTIVATION_BLOCKED, (String) null);
        this.blockReasons = blockReasons;
    }

    public List<String> getBlockReasons() {
        return blockReasons;
    }
}
