package com.metaforge.metamodel.domain.exception;

import static com.metaforge.metamodel.api.constants.ErrorCodes.VERSION_NOT_DRAFT;

/**
 * 非草稿态版本不可编辑异常。
 */
public class VersionNotDraftException extends BaseMetamodelException {

    public VersionNotDraftException(String fqn, String status) {
        super(VERSION_NOT_DRAFT, "版本 " + fqn + " 当前状态为 " + status + "，仅草稿态可编辑");
    }

    @Override
    public String getErrorCodeName() {
        return "VERSION_NOT_DRAFT";
    }
}
