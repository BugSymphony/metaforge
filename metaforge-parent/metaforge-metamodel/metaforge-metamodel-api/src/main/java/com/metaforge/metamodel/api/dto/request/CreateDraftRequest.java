package com.metaforge.metamodel.api.dto.request;

/**
 * 创建草稿版本请求 DTO。
 * bundleFqn 作为路径参数提供；仅需要 upgradeLevel。
 */
public class CreateDraftRequest {

    /** 目标 Bundle FQN */
    private String bundleFqn;

    /** 升级等级（PATCH/MINOR/MAJOR，默认 PATCH） */
    private String upgradeLevel = "PATCH";

    public String getBundleFqn() { return bundleFqn; }
    public void setBundleFqn(String bundleFqn) { this.bundleFqn = bundleFqn; }
    public String getUpgradeLevel() { return upgradeLevel; }
    public void setUpgradeLevel(String upgradeLevel) { this.upgradeLevel = upgradeLevel; }
}
