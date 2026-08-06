package com.metaforge.computeengine.infrastructure.config;

import com.metaforge.computeengine.domain.model.valueobject.TransitivityRule;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 语义查询引擎配置属性类。
 *
 * <p>绑定前缀为 metaforge.compute-engine 的 YAML 配置节点。
 * 包含遍历参数（最大深度、超时阈值、结果数量上限）与 AssociationType 传导规则列表。
 *
 * @author metaforge
 */
@ConfigurationProperties(prefix = "metaforge.compute-engine")
@Component
public class ComputeEngineProperties {

    private TraversalConfig traversal = new TraversalConfig();
    private List<TransitivityRuleConfig> transitivityRules = new ArrayList<>();

    public TraversalConfig getTraversal() {
        return traversal;
    }

    public void setTraversal(TraversalConfig traversal) {
        this.traversal = traversal;
    }

    public List<TransitivityRuleConfig> getTransitivityRules() {
        return transitivityRules;
    }

    public void setTransitivityRules(List<TransitivityRuleConfig> transitivityRules) {
        this.transitivityRules = transitivityRules;
    }

    /**
     * 遍历参数配置。
     */
    public static class TraversalConfig {
        private int maxDepth = 5;
        private long timeoutMs = 2000;
        private int maxResultCount = 500;

        public int getMaxDepth() {
            return maxDepth;
        }

        public void setMaxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
        }

        public long getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public int getMaxResultCount() {
            return maxResultCount;
        }

        public void setMaxResultCount(int maxResultCount) {
            this.maxResultCount = maxResultCount;
        }
    }

    /**
     * 单个 AssociationType 传导规则 YAML 映射。
     */
    public static class TransitivityRuleConfig {
        private String type;
        private boolean transitive = false;
        private String direction = "forward";
        private String weightStrategy;
        private int maxDepth = 1;
        private String description;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public boolean isTransitive() {
            return transitive;
        }

        public void setTransitive(boolean transitive) {
            this.transitive = transitive;
        }

        public String getDirection() {
            return direction;
        }

        public void setDirection(String direction) {
            this.direction = direction;
        }

        public String getWeightStrategy() {
            return weightStrategy;
        }

        public void setWeightStrategy(String weightStrategy) {
            this.weightStrategy = weightStrategy;
        }

        public int getMaxDepth() {
            return maxDepth;
        }

        public void setMaxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
