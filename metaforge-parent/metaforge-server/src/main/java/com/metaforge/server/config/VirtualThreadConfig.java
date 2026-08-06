package com.metaforge.server.config;

import java.util.concurrent.Executors;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;

/**
 * 虚拟线程配置
 * <p>将 Tomcat 协议处理器的执行器替换为虚拟线程执行器，
 * 以提升在高并发 I/O 密集型场景下的吞吐量。</p>
 */
@AutoConfiguration
public class VirtualThreadConfig {

    /**
     * 为 Tomcat 协议处理器配置虚拟线程执行器
     *
     * @return TomcatProtocolHandlerCustomizer 实例
     */
    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
        return protocolHandler -> protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }
}
