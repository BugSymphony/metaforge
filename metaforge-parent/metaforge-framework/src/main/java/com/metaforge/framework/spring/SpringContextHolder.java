package com.metaforge.framework.spring;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring 容器持有者，用于在非 Spring 管理的 Bean 中获取 ApplicationContext 和 Bean。
 */
@Component
public class SpringContextHolder implements ApplicationContextAware {

    /** 全局静态 ApplicationContext */
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
    }

    /**
     * 获取 ApplicationContext。
     *
     * @return 当前 Spring 容器上下文
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * 按类型获取 Bean。
     *
     * @param clazz Bean 类型
     * @param <T>   泛型
     * @return Bean 实例
     */
    public static <T> T getBean(Class<T> clazz) {
        return applicationContext.getBean(clazz);
    }

    /**
     * 按名称和类型获取 Bean。
     *
     * @param name  Bean 名称
     * @param clazz Bean 类型
     * @param <T>   泛型
     * @return Bean 实例
     */
    public static <T> T getBean(String name, Class<T> clazz) {
        return applicationContext.getBean(name, clazz);
    }

    /**
     * 按名称获取 Bean。
     *
     * @param name Bean 名称
     * @return Bean 实例（Object 类型）
     */
    public static Object getBean(String name) {
        return applicationContext.getBean(name);
    }
}
