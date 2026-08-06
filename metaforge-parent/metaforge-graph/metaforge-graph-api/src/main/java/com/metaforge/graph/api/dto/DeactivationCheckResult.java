package com.metaforge.graph.api.dto;

import java.util.List;

/**
 * 下线前置校验结果 DTO。
 */
public class DeactivationCheckResult {

    private boolean canDeprecate;
    private List<String> blockingRelations;

    public DeactivationCheckResult() {}

    public static DeactivationCheckResult pass() {
        DeactivationCheckResult result = new DeactivationCheckResult();
        result.canDeprecate = true;
        result.blockingRelations = List.of();
        return result;
    }

    public static DeactivationCheckResult blocked(List<String> blockingRelations) {
        DeactivationCheckResult result = new DeactivationCheckResult();
        result.canDeprecate = false;
        result.blockingRelations = blockingRelations;
        return result;
    }

    public boolean isCanDeprecate() { return canDeprecate; }
    public void setCanDeprecate(boolean canDeprecate) { this.canDeprecate = canDeprecate; }

    public List<String> getBlockingRelations() { return blockingRelations; }
    public void setBlockingRelations(List<String> blockingRelations) { this.blockingRelations = blockingRelations; }
}
