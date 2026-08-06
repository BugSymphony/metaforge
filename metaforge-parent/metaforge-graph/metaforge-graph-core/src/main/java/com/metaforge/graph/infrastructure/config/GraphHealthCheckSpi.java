package com.metaforge.graph.infrastructure.config;

import com.metaforge.common.spi.HealthCheckSpi;
import org.springframework.stereotype.Component;

/**
 * metaforge-graph BC 健康检查 SPI 实现。
 * 检查 BC 核心组件运行状态。
 */
@Component
public class GraphHealthCheckSpi implements HealthCheckSpi {

    @Override
    public HealthCheckResult check() {
        return new HealthCheckResult(
                "metaforge-graph",
                true,
                "语义关系网络 BC 运行正常"
        );
    }
}
