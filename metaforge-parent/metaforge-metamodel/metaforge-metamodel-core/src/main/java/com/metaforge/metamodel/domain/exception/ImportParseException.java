package com.metaforge.metamodel.domain.exception;

import static com.metaforge.metamodel.api.constants.ErrorCodes.IMPORT_PARSE_FAILED;

/**
 * 导入解析失败异常。
 */
public class ImportParseException extends BaseMetamodelException {

    public ImportParseException(String reason) {
        super(IMPORT_PARSE_FAILED, "导入解析失败: " + reason);
    }

    public ImportParseException(String reason, Throwable cause) {
        super(IMPORT_PARSE_FAILED, "导入解析失败: " + reason, cause);
    }

    @Override
    public String getErrorCodeName() {
        return "IMPORT_PARSE_FAILED";
    }
}
