package com.metaforge.server.config;

import java.util.Locale;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

/**
 * 国际化（i18n）配置
 * <p>配置基于 Accept-Language 请求头的区域解析器，默认语言为简体中文（zh-CN）。</p>
 */
@AutoConfiguration
public class I18nConfig implements WebMvcConfigurer {

    /**
     * 创建区域解析器
     * <p>使用 AcceptHeaderLocaleResolver，默认区域为 zh-CN。</p>
     *
     * @return LocaleResolver 实例
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.forLanguageTag("zh-CN"));
        return resolver;
    }

    /**
     * 创建区域变更拦截器
     * <p>解析请求中的 Accept-Language 头，自动切换区域。</p>
     *
     * @return LocaleChangeInterceptor 实例
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        return interceptor;
    }

    /**
     * 注册区域变更拦截器
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}
