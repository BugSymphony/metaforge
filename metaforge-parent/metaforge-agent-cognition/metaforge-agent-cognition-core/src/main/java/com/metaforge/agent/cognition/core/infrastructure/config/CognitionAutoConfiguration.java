package com.metaforge.agent.cognition.core.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CognitionConfigProperties.class)
public class CognitionAutoConfiguration {
}
