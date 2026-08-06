package com.metaforge.metamodel.domain.service;

import com.metaforge.metamodel.api.enums.ElementType;
import com.metaforge.metamodel.domain.model.valueobject.FqnParts;

/**
 * FQN 统一生成器接口。
 * 提供所有实体类型 FQN 的生成与解析能力，纯字符串变换，无状态无副作用。
 */
public interface FqnGenerator {

    // ========== 生成 ==========

    /** Bundle FQN: {code} */
    String bundle(String code);

    /** BundleVersion FQN: {code}:{version} */
    String bundleVersion(String code, String version);

    /** Package FQN: {parentFqn}.{segment} */
    String package_(String parentFqn, String segment);

    /** EntitySchema FQN: {packageFqn}.{segment} */
    String entitySchema(String packageFqn, String segment);

    /** RelationSchema FQN: {packageFqn}.{segment} */
    String relationSchema(String packageFqn, String segment);

    /** AttributeTemplate FQN: {bundleVersionFqn}.{segment} */
    String attributeTemplate(String bundleVersionFqn, String segment);

    // ========== 解析 ==========

    /** 解析纯净 FQN 为结构化部件 */
    FqnParts parse(String fqn);

    /** 获取父级 FQN（去掉最后一段） */
    String toParentFqn(String fqn);

    /** 获取短名（FQN 最后一段） */
    String toShortName(String fqn);

    /** 获取 Bundle Code（FQN 第一个 ':' 前部分） */
    String toBundleCode(String fqn);

    /** 获取版本号（':' 后前三个数字 segment） */
    String toVersion(String fqn);

    /** 文件系统映射：fqn.replace(":", "/").replace(".", "/") + ".json" */
    String toFilePath(String fqn);

    // ========== 类型前缀 ==========

    /** 剥离类型前缀，返回纯净 FQN */
    String stripTypePrefix(String typedFqn);

    /** 从带前缀的 FQN 检测实体类型 */
    ElementType detectType(String typedFqn);
}
