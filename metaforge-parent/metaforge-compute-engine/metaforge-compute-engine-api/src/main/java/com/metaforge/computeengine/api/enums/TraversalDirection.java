package com.metaforge.computeengine.api.enums;

/**
 * 遍历方向枚举。
 *
 * <p>定义图遍历中沿关系边扩展的方向。
 *
 * @author metaforge
 */
public enum TraversalDirection {

    FORWARD("正向，沿出边"),
    BACKWARD("反向，沿入边"),
    DIRECTED("单向，不隐含反向传递性"),
    BIDIRECTIONAL("双向");

    private final String description;

    TraversalDirection(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
