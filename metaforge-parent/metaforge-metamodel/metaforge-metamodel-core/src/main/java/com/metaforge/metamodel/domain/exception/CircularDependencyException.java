package com.metaforge.metamodel.domain.exception;

import java.util.List;

import static com.metaforge.metamodel.api.constants.ErrorCodes.CIRCULAR_DEPENDENCY;

/**
 * 循环依赖检测到环异常。
 */
public class CircularDependencyException extends BaseMetamodelException {

    public CircularDependencyException(List<List<String>> cycles) {
        super(CIRCULAR_DEPENDENCY,
                "检测到循环依赖: " + formatCycles(cycles));
    }

    private static String formatCycles(List<List<String>> cycles) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cycles.size(); i++) {
            if (i > 0) sb.append("; ");
            sb.append(String.join(" → ", cycles.get(i)));
        }
        return sb.toString();
    }

    @Override
    public String getErrorCodeName() {
        return "CIRCULAR_DEPENDENCY";
    }
}
