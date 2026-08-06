package com.metaforge.common.spi;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 序列化/反序列化扩展点接口
 * <p>用于扩展 Jackson 的序列化和反序列化配置，支持自定义序列化器和反序列化器。</p>
 *
 * @author metaforge
 */
public interface SerializationSpi {

    /**
     * 自定义序列化配置
     * <p>通过 ObjectMapper 注册自定义的序列化器、模块等配置。</p>
     *
     * @param mapper Jackson ObjectMapper 实例
     */
    default void customizeSerializer(ObjectMapper mapper) {
    }

    /**
     * 自定义反序列化配置
     * <p>通过 ObjectMapper 注册自定义的反序列化器、模块等配置。</p>
     *
     * @param mapper Jackson ObjectMapper 实例
     */
    default void customizeDeserializer(ObjectMapper mapper) {
    }
}
