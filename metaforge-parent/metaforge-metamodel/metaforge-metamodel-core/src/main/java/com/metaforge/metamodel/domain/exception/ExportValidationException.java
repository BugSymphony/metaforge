package com.metaforge.metamodel.domain.exception;

import static com.metaforge.metamodel.api.constants.ErrorCodes.EXPORT_VALIDATION_FAILED;

/**
 * 导出清单校验失败异常。
 */
public class ExportValidationException extends BaseMetamodelException {

    public ExportValidationException(String reason) {
        super(EXPORT_VALIDATION_FAILED, "导出清单校验失败: " + reason);
    }

    @Override
    public String getErrorCodeName() {
        return "EXPORT_VALIDATION_FAILED";
    }
}
