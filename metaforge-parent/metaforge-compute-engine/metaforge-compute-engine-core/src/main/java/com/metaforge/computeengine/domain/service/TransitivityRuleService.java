package com.metaforge.computeengine.domain.service;

import com.metaforge.computeengine.api.enums.AssociationType;
import com.metaforge.computeengine.api.enums.TraversalDirection;
import com.metaforge.computeengine.api.enums.WeightStrategy;
import com.metaforge.computeengine.domain.model.valueobject.TransitivityRule;
import com.metaforge.computeengine.infrastructure.config.ComputeEngineProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 传导规则加载与查询服务。
 *
 * <p>在应用启动时从 {@link ComputeEngineProperties} 加载 AssociationType 传导规则配置，
 * 提供类型安全的查询方法：判断传递性、获取有效深度、获取方向与权重策略。
 *
 * @author metaforge
 */
@Service
public class TransitivityRuleService {

    private static final Logger log = LoggerFactory.getLogger(TransitivityRuleService.class);

    private final ComputeEngineProperties properties;
    private Map<AssociationType, TransitivityRule> rules = Collections.emptyMap();

    public TransitivityRuleService(ComputeEngineProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        List<ComputeEngineProperties.TransitivityRuleConfig> configs = properties.getTransitivityRules();
        if (configs == null || configs.isEmpty()) {
            log.warn("未配置任何 AssociationType 传导规则，将使用空规则集");
            rules = Collections.emptyMap();
            return;
        }

        EnumMap<AssociationType, TransitivityRule> map = new EnumMap<>(AssociationType.class);
        for (ComputeEngineProperties.TransitivityRuleConfig config : configs) {
            try {
                AssociationType type = AssociationType.valueOf(config.getType());
                TraversalDirection direction = parseDirection(config.getDirection());
                WeightStrategy weightStrategy = parseWeightStrategy(config.getWeightStrategy());
                TransitivityRule rule = new TransitivityRule(
                        type, config.isTransitive(), direction, weightStrategy,
                        config.getMaxDepth(), config.getDescription());
                map.put(type, rule);
            } catch (Exception e) {
                log.warn("解析传导规则配置失败，跳过 type={}，原因: {}", config.getType(), e.getMessage());
            }
        }
        rules = Collections.unmodifiableMap(map);
        log.info("已加载 {} 条 AssociationType 传导规则", rules.size());
    }

    /**
     * 获取指定类型的传导规则。
     *
     * @param type AssociationType
     * @return 传导规则，未配置时返回 null
     */
    public TransitivityRule getRule(AssociationType type) {
        return rules.get(type);
    }

    /**
     * 判断指定类型是否可传递。
     */
    public boolean isTransitive(AssociationType type) {
        TransitivityRule rule = rules.get(type);
        return rule != null && rule.isTransitive();
    }

    /**
     * 获取指定类型的有效最大深度，与全局深度取较小值。
     */
    public int getEffectiveMaxDepth(AssociationType type, int globalMax) {
        TransitivityRule rule = rules.get(type);
        if (rule != null) {
            return Math.min(globalMax, rule.getMaxDepth());
        }
        return globalMax;
    }

    /**
     * 获取指定类型的遍历方向。
     */
    public TraversalDirection getDirection(AssociationType type) {
        TransitivityRule rule = rules.get(type);
        return rule != null ? rule.getDirection() : TraversalDirection.FORWARD;
    }

    /**
     * 获取指定类型的权重策略。
     */
    public WeightStrategy getWeightStrategy(AssociationType type) {
        TransitivityRule rule = rules.get(type);
        return rule != null ? rule.getWeightStrategy() : WeightStrategy.NONE;
    }

    /**
     * 获取所有已加载的传导规则。
     */
    public Map<AssociationType, TransitivityRule> getAllRules() {
        return rules;
    }

    private TraversalDirection parseDirection(String direction) {
        if (direction == null) return TraversalDirection.FORWARD;
        return switch (direction.toLowerCase()) {
            case "forward" -> TraversalDirection.FORWARD;
            case "backward" -> TraversalDirection.BACKWARD;
            case "directed" -> TraversalDirection.DIRECTED;
            case "bidirectional" -> TraversalDirection.BIDIRECTIONAL;
            default -> TraversalDirection.FORWARD;
        };
    }

    private WeightStrategy parseWeightStrategy(String strategy) {
        if (strategy == null) return WeightStrategy.NONE;
        return switch (strategy.toLowerCase()) {
            case "multiply" -> WeightStrategy.MULTIPLY;
            case "add" -> WeightStrategy.ADD;
            case "max" -> WeightStrategy.MAX;
            default -> WeightStrategy.NONE;
        };
    }
}
