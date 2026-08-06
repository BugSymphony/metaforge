package com.metaforge.agent.cognition.core.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "metaforge.agent-cognition")
public class AgentCognitionProperties {

    private String templatePath;
    private String perspectivesPath;
    private boolean cacheEnabled = true;
    private int cacheExpireMinutes = 30;
    private int perspectiveTimeoutMs = 200;
    private int defaultMaxTokens = 8000;
    private String defaultDepth = "L2";
    private String defaultArchetype = "execution";

    public String getTemplatePath() { return templatePath; }
    public void setTemplatePath(String templatePath) { this.templatePath = templatePath; }

    public String getPerspectivesPath() { return perspectivesPath; }
    public void setPerspectivesPath(String perspectivesPath) { this.perspectivesPath = perspectivesPath; }

    public boolean isCacheEnabled() { return cacheEnabled; }
    public void setCacheEnabled(boolean cacheEnabled) { this.cacheEnabled = cacheEnabled; }

    public int getCacheExpireMinutes() { return cacheExpireMinutes; }
    public void setCacheExpireMinutes(int cacheExpireMinutes) { this.cacheExpireMinutes = cacheExpireMinutes; }

    public int getPerspectiveTimeoutMs() { return perspectiveTimeoutMs; }
    public void setPerspectiveTimeoutMs(int perspectiveTimeoutMs) { this.perspectiveTimeoutMs = perspectiveTimeoutMs; }

    public int getDefaultMaxTokens() { return defaultMaxTokens; }
    public void setDefaultMaxTokens(int defaultMaxTokens) { this.defaultMaxTokens = defaultMaxTokens; }

    public String getDefaultDepth() { return defaultDepth; }
    public void setDefaultDepth(String defaultDepth) { this.defaultDepth = defaultDepth; }

    public String getDefaultArchetype() { return defaultArchetype; }
    public void setDefaultArchetype(String defaultArchetype) { this.defaultArchetype = defaultArchetype; }
}
