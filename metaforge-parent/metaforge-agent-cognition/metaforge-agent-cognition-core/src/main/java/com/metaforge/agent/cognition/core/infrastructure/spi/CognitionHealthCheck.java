package com.metaforge.agent.cognition.core.infrastructure.spi;

import com.metaforge.common.spi.HealthCheckSpi;
import com.metaforge.agent.cognition.core.infrastructure.config.TemplateConfig;
import com.metaforge.agent.cognition.core.infrastructure.config.PerspectiveConfig;
import org.springframework.stereotype.Component;

@Component
public class CognitionHealthCheck implements HealthCheckSpi {

    private final TemplateConfig templateConfig;
    private final PerspectiveConfig perspectiveConfig;

    public CognitionHealthCheck(TemplateConfig templateConfig, PerspectiveConfig perspectiveConfig) {
        this.templateConfig = templateConfig;
        this.perspectiveConfig = perspectiveConfig;
    }

    @Override
    public HealthCheckResult check() {
        boolean templatesOk = templateConfig != null && templateConfig.getAllTemplates() != null
                && !templateConfig.getAllTemplates().isEmpty();
        boolean perspectivesOk = perspectiveConfig != null && perspectiveConfig.getPerspectives() != null
                && !perspectiveConfig.getPerspectives().isEmpty();

        if (!templatesOk || !perspectivesOk) {
            return new HealthCheckResult(
                    "agent-cognition",
                    false,
                    String.format("YAML 配置加载异常: templates=%s, perspectives=%s",
                            templatesOk ? "OK" : "FAILED",
                            perspectivesOk ? "OK" : "FAILED")
            );
        }
        return new HealthCheckResult(
                "agent-cognition",
                true,
                "元认知指导层 BC 运行正常，YAML 配置加载完成"
        );
    }
}
