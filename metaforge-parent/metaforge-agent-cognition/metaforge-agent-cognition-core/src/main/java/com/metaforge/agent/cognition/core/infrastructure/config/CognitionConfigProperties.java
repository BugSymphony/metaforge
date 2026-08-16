package com.metaforge.agent.cognition.core.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "metaforge.agent-cognition")
public class CognitionConfigProperties {

    private final Templates templates = new Templates();
    private final Defaults defaults = new Defaults();
    private final Timeouts timeouts = new Timeouts();
    private final Depth depth = new Depth();
    private final VersionAnchor versionAnchor = new VersionAnchor();

    public Templates getTemplates() { return templates; }
    public Defaults getDefaults() { return defaults; }
    public Timeouts getTimeouts() { return timeouts; }
    public Depth getDepth() { return depth; }
    public VersionAnchor getVersionAnchor() { return versionAnchor; }

    public static class Templates {
        private String classpathLocation = "classpath:cognition/templates/";
        private String externalLocation = "";
        private final HotReload hotReload = new HotReload();

        public String getClasspathLocation() { return classpathLocation; }
        public void setClasspathLocation(String classpathLocation) { this.classpathLocation = classpathLocation; }
        public String getExternalLocation() { return externalLocation; }
        public void setExternalLocation(String externalLocation) { this.externalLocation = externalLocation; }
        public HotReload getHotReload() { return hotReload; }

        public static class HotReload {
            private boolean enabled = false;
            private long pollIntervalMs = 5000;

            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            public long getPollIntervalMs() { return pollIntervalMs; }
            public void setPollIntervalMs(long pollIntervalMs) { this.pollIntervalMs = pollIntervalMs; }
        }
    }

    public static class Defaults {
        private String cognitionDepth = "L2";
        private String agentArchetype = "execution";
        private String format = "json";
        private int maxTokens = 8000;
        private int pageSize = 20;

        public String getCognitionDepth() { return cognitionDepth; }
        public void setCognitionDepth(String cognitionDepth) { this.cognitionDepth = cognitionDepth; }
        public String getAgentArchetype() { return agentArchetype; }
        public void setAgentArchetype(String agentArchetype) { this.agentArchetype = agentArchetype; }
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
        public int getPageSize() { return pageSize; }
        public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    }

    public static class Timeouts {
        private long operatorExecuteDefaultMs = 10000;

        public long getOperatorExecuteDefaultMs() { return operatorExecuteDefaultMs; }
        public void setOperatorExecuteDefaultMs(long operatorExecuteDefaultMs) { this.operatorExecuteDefaultMs = operatorExecuteDefaultMs; }
    }

    public static class Depth {
        private double trimRatioL1 = 0.33;
        private double trimRatioL2 = 0.67;
        private int minKeep = 3;

        public double getTrimRatioL1() { return trimRatioL1; }
        public void setTrimRatioL1(double trimRatioL1) { this.trimRatioL1 = trimRatioL1; }
        public double getTrimRatioL2() { return trimRatioL2; }
        public void setTrimRatioL2(double trimRatioL2) { this.trimRatioL2 = trimRatioL2; }
        public int getMinKeep() { return minKeep; }
        public void setMinKeep(int minKeep) { this.minKeep = minKeep; }
    }

    public static class VersionAnchor {
        private String bundleResolveStrategy = "LATEST_PUBLISHED";

        public String getBundleResolveStrategy() { return bundleResolveStrategy; }
        public void setBundleResolveStrategy(String bundleResolveStrategy) { this.bundleResolveStrategy = bundleResolveStrategy; }
    }
}
