package com.metaforge.common.spi;

/**
 * 日志脱敏扩展点接口
 * <p>用于对日志输出中的敏感字段进行脱敏处理，防止敏感信息泄露。</p>
 *
 * @author metaforge
 */
@FunctionalInterface
public interface LogMaskSpi {

    /**
     * 对指定字段值进行脱敏处理
     *
     * @param fieldName  字段名称
     * @param fieldValue 字段原始值
     * @return 脱敏后的值
     */
    String mask(String fieldName, String fieldValue);
}
