package com.metaforge.metamodel.domain.exception;

import static com.metaforge.metamodel.api.constants.ErrorCodes.PREDEFINED_BUNDLE_PROTECTED;

/**
 * 预置 Bundle 受保护异常：禁止删除/修改系统内置 Bundle。
 */
public class PredefinedBundleProtectedException extends BaseMetamodelException {

    public PredefinedBundleProtectedException(String bundleFqn, String operation) {
        super(PREDEFINED_BUNDLE_PROTECTED,
                "预置 Bundle " + bundleFqn + " 受保护，禁止执行 " + operation + " 操作");
    }

    @Override
    public String getErrorCodeName() {
        return "PREDEFINED_BUNDLE_PROTECTED";
    }
}
