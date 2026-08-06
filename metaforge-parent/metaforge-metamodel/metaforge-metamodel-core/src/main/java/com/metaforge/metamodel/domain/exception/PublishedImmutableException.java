package com.metaforge.metamodel.domain.exception;

import static com.metaforge.metamodel.api.constants.ErrorCodes.PUBLISHED_IMMUTABLE;

/**
 * 已发布版本不可修改异常。
 */
public class PublishedImmutableException extends BaseMetamodelException {

    public PublishedImmutableException(String fqn) {
        super(PUBLISHED_IMMUTABLE,
                "已发布版本不可修改: " + fqn + " 已处于 PUBLISHED 状态，所有字段只读");
    }

    public PublishedImmutableException(String fqn, String operation) {
        super(PUBLISHED_IMMUTABLE,
                "已发布版本禁止 " + operation + " 操作: " + fqn);
    }

    @Override
    public String getErrorCodeName() {
        return "PUBLISHED_IMMUTABLE";
    }
}
