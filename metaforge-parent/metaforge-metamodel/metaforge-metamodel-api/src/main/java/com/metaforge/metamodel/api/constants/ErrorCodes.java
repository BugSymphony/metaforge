package com.metaforge.metamodel.api.constants;

/**
 * 元模型治理 BC 错误码常量定义。
 *
 * <p>错误码范围 30101-30112，为 metamodel-governance BC 独占分配范围。
 * 所有业务异常均通过本类常量引用，禁止在代码中硬编码错误码数值。
 */
public final class ErrorCodes {

    private ErrorCodes() {
        throw new UnsupportedOperationException("常量类不可实例化");
    }

    /** FQN 全局重复 */
    public static final int FQN_DUPLICATE = 30101;

    /** FQN 引用目标不存在 */
    public static final int FQN_NOT_FOUND = 30102;

    /** 非草稿态版本不可编辑 */
    public static final int VERSION_NOT_DRAFT = 30103;

    /** 升级等级与变更不匹配 */
    public static final int UPGRADE_LEVEL_MISMATCH = 30104;

    /** 循环依赖检测到环 */
    public static final int CIRCULAR_DEPENDENCY = 30105;

    /** 属性名冲突 */
    public static final int ATTR_NAME_CONFLICT = 30106;

    /** Package 嵌套深度超限 */
    public static final int PACKAGE_DEPTH_EXCEEDED = 30107;

    /** 导出清单校验失败 */
    public static final int EXPORT_VALIDATION_FAILED = 30108;

    /** 已发布版本不可修改 */
    public static final int PUBLISHED_IMMUTABLE = 30109;

    /** 预置 Bundle 受保护，不可删除/修改 */
    public static final int PREDEFINED_BUNDLE_PROTECTED = 30110;

    /** 依赖目标不存在 */
    public static final int DEPENDENCY_TARGET_NOT_FOUND = 30111;

    /** 导入解析失败 */
    public static final int IMPORT_PARSE_FAILED = 30112;
}
