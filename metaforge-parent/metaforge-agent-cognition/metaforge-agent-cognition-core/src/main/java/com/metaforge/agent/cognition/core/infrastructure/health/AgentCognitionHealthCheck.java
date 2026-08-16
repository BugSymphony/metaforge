package com.metaforge.agent.cognition.core.infrastructure.health;

import com.metaforge.agent.cognition.core.infrastructure.registry.TemplateRegistry;
import com.metaforge.common.spi.HealthCheckSpi;
import org.springframework.stereotype.Component;

@Component
public class AgentCognitionHealthCheck implements HealthCheckSpi {

    private final TemplateRegistry templateRegistry;

    public AgentCognitionHealthCheck(TemplateRegistry templateRegistry) {
        this.templateRegistry = templateRegistry;
    }

    @Override
    public HealthCheckResult check() {
        int registeredCount = templateRegistry.size();
        boolean hasBuiltInTemplates = registeredCount >= 6;

        if (hasBuiltInTemplates) {
            return new HealthCheckResult(
                    "agentCognitionTemplates",
                    true,
                    "6+ built-in templates registered (" + registeredCount + " total)"
            );
        } else {
            return new HealthCheckResult(
                    "agentCognitionTemplates",
                    false,
                    "Only " + registeredCount + "/6 built-in templates registered"
            );
        }
    }
}
