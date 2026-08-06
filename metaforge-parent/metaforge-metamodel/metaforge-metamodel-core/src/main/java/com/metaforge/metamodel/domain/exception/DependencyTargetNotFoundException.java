package com.metaforge.metamodel.domain.exception;

import static com.metaforge.metamodel.api.constants.ErrorCodes.DEPENDENCY_TARGET_NOT_FOUND;

/**
 * 依赖目标不存在的异常。
 */
public class DependencyTargetNotFoundException extends BaseMetamodelException {

    public DependencyTargetNotFoundException(String targetVersionFqn) {
        super(DEPENDENCY_TARGET_NOT_FOUND,
                "依赖目标版本不存在或未发布: " + targetVersionFqn);
    }

    @Override
    public String getErrorCodeName() {
        return "DEPENDENCY_TARGET_NOT_FOUND";
    }
}
