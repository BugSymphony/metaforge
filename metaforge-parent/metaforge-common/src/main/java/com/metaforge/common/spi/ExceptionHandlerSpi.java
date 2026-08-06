package com.metaforge.common.spi;

import com.metaforge.common.dto.ApiResponse;

/**
 * 异常处理扩展点接口
 * <p>用于处理特定类型的异常，返回统一的 API 响应对象。
 * 如果返回 null 则表示不处理该异常，交由下一个处理器处理。</p>
 *
 * @author metaforge
 */
@FunctionalInterface
public interface ExceptionHandlerSpi {

    /**
     * 处理异常
     *
     * @param e 需要处理的异常对象
     * @return 处理后的 ApiResponse；返回 null 表示不处理，交由下一个处理器
     */
    ApiResponse<?> handle(Exception e);
}
