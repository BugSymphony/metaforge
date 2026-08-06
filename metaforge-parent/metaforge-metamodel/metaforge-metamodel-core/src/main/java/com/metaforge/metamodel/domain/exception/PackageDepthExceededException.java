package com.metaforge.metamodel.domain.exception;

import static com.metaforge.metamodel.api.constants.ErrorCodes.PACKAGE_DEPTH_EXCEEDED;

/**
 * Package 嵌套深度超限异常（上限 5 层）。
 */
public class PackageDepthExceededException extends BaseMetamodelException {

    public PackageDepthExceededException(int depth, int maxDepth) {
        super(PACKAGE_DEPTH_EXCEEDED,
                "Package 嵌套深度超限: 当前深度 " + depth + "，上限 " + maxDepth + " 层");
    }

    @Override
    public String getErrorCodeName() {
        return "PACKAGE_DEPTH_EXCEEDED";
    }
}
