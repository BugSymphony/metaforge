package com.metaforge.computeengine.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 开放主机服务语义标记。
 *
 * <p>标记 Application Service 接口为本 BC 的开放主机服务（OHS），
 * 对外暴露为可被下游 BC 依赖调用的进程内服务契约。
 *
 * @author metaforge
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface OpenHostService {
}
