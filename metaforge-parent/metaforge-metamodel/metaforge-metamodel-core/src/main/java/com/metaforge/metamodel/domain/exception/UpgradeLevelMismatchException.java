package com.metaforge.metamodel.domain.exception;

import com.metaforge.metamodel.api.enums.UpgradeLevel;

import static com.metaforge.metamodel.api.constants.ErrorCodes.UPGRADE_LEVEL_MISMATCH;

/**
 * 升级等级与变更内容不匹配异常。
 */
public class UpgradeLevelMismatchException extends BaseMetamodelException {

    public UpgradeLevelMismatchException(UpgradeLevel declared, String reason) {
        super(UPGRADE_LEVEL_MISMATCH,
                "升级等级 " + declared + " 与变更内容不匹配: " + reason);
    }

    @Override
    public String getErrorCodeName() {
        return "UPGRADE_LEVEL_MISMATCH";
    }
}
