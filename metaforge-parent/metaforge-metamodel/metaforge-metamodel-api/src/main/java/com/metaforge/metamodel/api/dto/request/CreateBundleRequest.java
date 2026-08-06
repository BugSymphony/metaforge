package com.metaforge.metamodel.api.dto.request;

/**
 * 创建 Bundle 请求 DTO。
 */
public class CreateBundleRequest {

    /** Bundle FQN，需匹配正则 [a-z][a-z0-9_-]{2,63} */
    private String fqn;

    /** 人类可读名称 */
    private String name;

    /** 描述（必填） */
    private String description;

    /** 负责人 */
    private String owner;

    public String getFqn() { return fqn; }
    public void setFqn(String fqn) { this.fqn = fqn; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
}
