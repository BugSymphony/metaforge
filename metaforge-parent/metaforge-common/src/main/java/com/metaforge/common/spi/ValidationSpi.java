package com.metaforge.common.spi;

/**
 * 校验器扩展点接口
 * <p>用于注册自定义校验器，扩展系统的数据校验能力。</p>
 *
 * @author metaforge
 */
public interface ValidationSpi {

    /**
     * 注册自定义校验器
     *
     * @param registry 校验器注册中心对象
     */
    default void registerValidators(Object registry) {
    }
}
