package com.metaforge.agent.cognition.api.constant;

/**
 * 元认知指导层 BC 错误码常量（34001-34006）。
 * 错误码段 34000-34099 为本 BC 专属。
 */
public final class AgentCognitionErrorCodes {

    private AgentCognitionErrorCodes() {}

    /** 模板未找到 — 请求的 templateId 在 YAML 配置中不存在 */
    public static final int TEMPLATE_NOT_FOUND = 34001;

    /** Bundle FQN 格式非法 — bundle_fqns 不符合 FQN 格式 */
    public static final int INVALID_BUNDLE_FQN = 34002;

    /** Bundle FQN 列表为空 — bundle_fqns 参数为空 */
    public static final int EMPTY_BUNDLE_FQNS = 34003;

    /** 实体 FQN 非法 — entity_fqn 前缀不属于任何已发布 Bundle */
    public static final int INVALID_ENTITY_FQN = 34004;

    /** 视角查询超时 — 单个视角查询超过 200ms 阈值 */
    public static final int PERSPECTIVE_TIMEOUT = 34005;

    /** 上游 BC 不可用 — 上游 BC 服务调用失败 */
    public static final int UPSTREAM_UNAVAILABLE = 34006;

    /** 作用域模式非法 — scope_mode 不是 INHERITED/PURE */
    public static final int SCOPE_MODE_INVALID = 34007;
}
