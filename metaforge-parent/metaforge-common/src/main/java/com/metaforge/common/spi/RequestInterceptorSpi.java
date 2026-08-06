package com.metaforge.common.spi;

/**
 * 请求拦截器扩展点接口
 * <p>在 HTTP 请求处理的不同阶段执行拦截逻辑，支持请求前置处理、后置处理和完成回调。
 * 方法参数类型实际为 jakarta.servlet.http.HttpServletRequest / HttpServletResponse，
 * 此处使用 Object 避免 common 层引入 Servlet API 依赖，调用方需自行做类型断言。</p>
 *
 * @author metaforge
 */
public interface RequestInterceptorSpi {

    /**
     * 请求前置处理
     * <p>在请求到达控制器之前执行，返回 false 将中断请求链。</p>
     *
     * @param request HTTP 请求对象（实际类型为 jakarta.servlet.http.HttpServletRequest）
     * @return true 继续处理；false 中断请求链
     */
    default boolean preHandle(Object request) {
        return true;
    }

    /**
     * 请求后置处理
     * <p>在控制器方法执行完成后、视图渲染之前执行。</p>
     *
     * @param request  HTTP 请求对象（实际类型为 jakarta.servlet.http.HttpServletRequest）
     * @param response HTTP 响应对象（实际类型为 jakarta.servlet.http.HttpServletResponse）
     */
    default void postHandle(Object request, Object response) {
    }

    /**
     * 请求完成回调
     * <p>在请求处理完成后执行，无论成功或异常都会调用。</p>
     *
     * @param request  HTTP 请求对象
     * @param response HTTP 响应对象
     * @param ex       处理过程中发生的异常，无异常时为 null
     */
    default void afterCompletion(Object request, Object response, Exception ex) {
    }
}
