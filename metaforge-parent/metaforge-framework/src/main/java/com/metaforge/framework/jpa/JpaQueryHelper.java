package com.metaforge.framework.jpa;

import java.util.Collection;

import org.springframework.data.jpa.domain.Specification;

/**
 * JPA 动态查询辅助工具，基于 Spring Data JPA Specification 构建常用查询条件。
 */
public final class JpaQueryHelper {

    private JpaQueryHelper() {
    }

    /**
     * 构建等值查询条件。value 为 null 时条件不生效。
     *
     * @param fieldName 实体字段名
     * @param value     匹配值
     * @param <T>       实体类型
     * @return Specification 对象
     */
    public static <T> Specification<T> equals(String fieldName, Object value) {
        return (root, query, cb) -> value == null ? null : cb.equal(root.get(fieldName), value);
    }

    /**
     * 构建模糊查询条件（前后加 %）。value 为 null 或空字符串时条件不生效。
     *
     * @param fieldName 实体字段名
     * @param value     模糊匹配值
     * @param <T>       实体类型
     * @return Specification 对象
     */
    public static <T> Specification<T> like(String fieldName, String value) {
        return (root, query, cb) -> {
            if (value == null || value.isEmpty()) {
                return null;
            }
            return cb.like(root.get(fieldName), "%" + value + "%");
        };
    }

    /**
     * 构建 IN 查询条件。values 为 null 或空集合时条件不生效。
     *
     * @param fieldName 实体字段名
     * @param values    匹配值集合
     * @param <T>       实体类型
     * @return Specification 对象
     */
    public static <T> Specification<T> in(String fieldName, Collection<?> values) {
        return (root, query, cb) -> {
            if (values == null || values.isEmpty()) {
                return null;
            }
            return root.get(fieldName).in(values);
        };
    }

    /**
     * 组合多个 Specification，使用 AND 连接。
     *
     * @param specs 多个 Specification
     * @param <T>   实体类型
     * @return 组合后的 Specification
     */
    @SafeVarargs
    public static <T> Specification<T> and(Specification<T>... specs) {
        return Specification.allOf(specs);
    }
}
