package com.metaforge.agent.cognition.api.constants;

public final class AgentCognitionErrorCodes {

    private AgentCognitionErrorCodes() {
    }

    public static final int TEMPLATE_NOT_FOUND = 34001;
    public static final String TEMPLATE_NOT_FOUND_MSG = "模板未注册或不可用";

    public static final int INVALID_SCOPE = 34003;
    public static final String INVALID_SCOPE_MSG = "scope 中声明的 bundle/package/entitySchema 无效或不存在";

    public static final int MISSING_SCOPE = 34005;
    public static final String MISSING_SCOPE_MSG = "模板要求 scope 但请求未提供";

    public static final int ENTITY_OUT_OF_SCOPE = 34004;
    public static final String ENTITY_OUT_OF_SCOPE_MSG = "请求中的 entityFqn 不在声明的 scope 范围内";

    public static final int ARCHETYPE_NOT_SUPPORTED = 34012;
    public static final String ARCHETYPE_NOT_SUPPORTED_MSG = "请求的 agentArchetype 不被该模板的任一算子支持";

    public static final int OPERATOR_EXECUTION_ERROR = 34006;
    public static final String OPERATOR_EXECUTION_ERROR_MSG = "算子执行异常";

    public static final int OPERATOR_TIMEOUT = 34007;
    public static final String OPERATOR_TIMEOUT_MSG = "算子执行超时";

    public static final int INVALID_FORMAT = 34010;
    public static final String INVALID_FORMAT_MSG = "请求指定的 output format 不被支持";

    public static final int UPSTREAM_UNAVAILABLE = 34008;
    public static final String UPSTREAM_UNAVAILABLE_MSG = "上游 BC 服务不可用或超时";

    public static final int TEMPLATE_INVALID = 34002;
    public static final String TEMPLATE_INVALID_MSG = "模板定义不合法（格式错误、operator 不存在等）";

    public static final int UNSUPPORTED_OPERATOR = 34009;
    public static final String UNSUPPORTED_OPERATOR_MSG = "模板引用的 operatorId 无匹配的注册算子";

    public static final int UNKNOWN_OPERATOR_REF = 34011;
    public static final String UNKNOWN_OPERATOR_REF_MSG = "模板中 operatorId 引用的分类前缀不存在";

    public static final int INVALID_LEVEL = 34013;
    public static final String INVALID_LEVEL_MSG = "请求指定的 level 无法解析为有效 EntitySchema 类型";

    public static final int INVALID_OPERATOR_SELECTION = 34014;
    public static final String INVALID_OPERATOR_SELECTION_MSG = "请求指定的 operators 无任何算子匹配模板声明";

    public static String getMessage(int code) {
        return switch (code) {
            case TEMPLATE_NOT_FOUND -> TEMPLATE_NOT_FOUND_MSG;
            case INVALID_SCOPE -> INVALID_SCOPE_MSG;
            case MISSING_SCOPE -> MISSING_SCOPE_MSG;
            case ENTITY_OUT_OF_SCOPE -> ENTITY_OUT_OF_SCOPE_MSG;
            case ARCHETYPE_NOT_SUPPORTED -> ARCHETYPE_NOT_SUPPORTED_MSG;
            case OPERATOR_EXECUTION_ERROR -> OPERATOR_EXECUTION_ERROR_MSG;
            case OPERATOR_TIMEOUT -> OPERATOR_TIMEOUT_MSG;
            case INVALID_FORMAT -> INVALID_FORMAT_MSG;
            case UPSTREAM_UNAVAILABLE -> UPSTREAM_UNAVAILABLE_MSG;
            case TEMPLATE_INVALID -> TEMPLATE_INVALID_MSG;
            case UNSUPPORTED_OPERATOR -> UNSUPPORTED_OPERATOR_MSG;
            case UNKNOWN_OPERATOR_REF -> UNKNOWN_OPERATOR_REF_MSG;
            case INVALID_LEVEL -> INVALID_LEVEL_MSG;
            case INVALID_OPERATOR_SELECTION -> INVALID_OPERATOR_SELECTION_MSG;
            default -> "未知错误";
        };
    }
}
