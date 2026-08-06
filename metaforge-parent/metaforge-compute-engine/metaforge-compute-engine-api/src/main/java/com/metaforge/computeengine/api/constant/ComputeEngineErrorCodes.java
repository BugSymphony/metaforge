package com.metaforge.computeengine.api.constant;

/**
 * 语义查询与推理引擎错误码常量类。
 *
 * <p>错误码范围 33000-33999，全局唯一。各常量定义错误码、消息模板以及建议的 HTTP 状态码。
 *
 * @author metaforge
 */
public final class ComputeEngineErrorCodes {

    private ComputeEngineErrorCodes() {
    }

    /** 实体不存在 */
    public static final int ENTITY_NOT_FOUND = 33001;
    public static final String ENTITY_NOT_FOUND_MSG = "查询起点实体 FQN 不存在或已下线";

    /** 遍历深度超限 */
    public static final int TRAVERSAL_DEPTH_EXCEEDED = 33002;
    public static final String TRAVERSAL_DEPTH_EXCEEDED_MSG = "遍历深度超过配置上限";

    /** 查询超时中断 */
    public static final int QUERY_TIMEOUT = 33003;
    public static final String QUERY_TIMEOUT_MSG = "查询执行超时，已中断";

    /** 结果数量超限 */
    public static final int RESULT_COUNT_EXCEEDED = 33004;
    public static final String RESULT_COUNT_EXCEEDED_MSG = "查询结果数量超过配置上限";

    /** 图模式匹配语法非法 */
    public static final int INVALID_PATTERN = 33005;
    public static final String INVALID_PATTERN_MSG = "图模式匹配语法非法或不符合长度限制";

    /** 过滤参数组合非法 */
    public static final int INVALID_FILTER = 33006;
    public static final String INVALID_FILTER_MSG = "过滤参数组合非法或包含无效值";

    /** 无合法传导路径 */
    public static final int NO_LEGAL_CONDUCTION_PATH = 33007;
    public static final String NO_LEGAL_CONDUCTION_PATH_MSG = "无可用的合法传导路径";

    /** 模式长度超过最大段数 */
    public static final int PATTERN_LENGTH_EXCEEDED = 33008;
    public static final String PATTERN_LENGTH_EXCEEDED_MSG = "图模式路径段数超过最大限制（4 段）";

    /** 批量查询 FQN 数量超限 */
    public static final int BATCH_SIZE_EXCEEDED = 33009;
    public static final String BATCH_SIZE_EXCEEDED_MSG = "批量查询 FQN 数量超过上限（200）";

    /** 上游模块不可用 */
    public static final int UPSTREAM_SERVICE_UNAVAILABLE = 33010;
    public static final String UPSTREAM_SERVICE_UNAVAILABLE_MSG = "上游 BC 模块不可用，查询中止";

    /** 循环引用已检测并截断 */
    public static final int CIRCULAR_REFERENCE_DETECTED = 33011;
    public static final String CIRCULAR_REFERENCE_DETECTED_MSG = "遍历过程中检测到循环引用，已自动截断";
}
