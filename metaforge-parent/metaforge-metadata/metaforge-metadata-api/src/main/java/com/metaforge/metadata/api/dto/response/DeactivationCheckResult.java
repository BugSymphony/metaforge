package com.metaforge.metadata.api.dto.response;

import java.util.List;

public class DeactivationCheckResult {
    private boolean canDeactivate;
    private List<String> blockReasons;
    private List<String> activeReferences;
    private List<String> activeChildren;

    public DeactivationCheckResult() {}

    public boolean isCanDeactivate() { return canDeactivate; }
    public void setCanDeactivate(boolean canDeactivate) { this.canDeactivate = canDeactivate; }
    public List<String> getBlockReasons() { return blockReasons; }
    public void setBlockReasons(List<String> blockReasons) { this.blockReasons = blockReasons; }
    public List<String> getActiveReferences() { return activeReferences; }
    public void setActiveReferences(List<String> activeReferences) { this.activeReferences = activeReferences; }
    public List<String> getActiveChildren() { return activeChildren; }
    public void setActiveChildren(List<String> activeChildren) { this.activeChildren = activeChildren; }
}
