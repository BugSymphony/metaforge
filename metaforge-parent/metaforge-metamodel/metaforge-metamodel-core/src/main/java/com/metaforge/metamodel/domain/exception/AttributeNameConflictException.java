package com.metaforge.metamodel.domain.exception;

import static com.metaforge.metamodel.api.constants.ErrorCodes.ATTR_NAME_CONFLICT;

/**
 * 属性名冲突异常。
 */
public class AttributeNameConflictException extends BaseMetamodelException {

    public AttributeNameConflictException(String attributeName) {
        super(ATTR_NAME_CONFLICT,
                "属性名冲突: " + attributeName + " 已存在，属性名在同一实体内必须唯一");
    }

    public AttributeNameConflictException(String attributeName, String entityFqn) {
        super(ATTR_NAME_CONFLICT,
                "属性名冲突: " + attributeName + " 在实体 " + entityFqn + " 内已存在");
    }

    @Override
    public String getErrorCodeName() {
        return "ATTR_NAME_CONFLICT";
    }
}
