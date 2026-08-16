package com.metaforge.agent.cognition.core.infrastructure.registry;

import com.metaforge.agent.cognition.api.enums.DimensionCategory;
import com.metaforge.agent.cognition.api.spi.CognitionOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OperatorRegistry {

    private static final Logger log = LoggerFactory.getLogger(OperatorRegistry.class);

    private final ConcurrentHashMap<String, CognitionOperator> cache = new ConcurrentHashMap<>();

    public void registerAll(List<CognitionOperator> operators) {
        if (operators == null || operators.isEmpty()) {
            log.info("无 CognitionOperator Bean 待注册");
            return;
        }

        Set<String> seenIds = new HashSet<>();
        for (CognitionOperator operator : operators) {
            try {
                validateOperator(operator, seenIds);
                cache.put(operator.operatorId(), operator);
                seenIds.add(operator.operatorId());
                log.debug("算子已注册: {} (category={})", operator.operatorId(), operator.category());
            } catch (IllegalArgumentException e) {
                log.warn("算子注册跳过: {}", e.getMessage());
            }
        }

        log.info("OperatorRegistry 就绪: {} 个算子已注册", cache.size());
    }

    public CognitionOperator resolve(String operatorId) {
        return cache.get(operatorId);
    }

    public Map<String, CognitionOperator> listAll() {
        return new LinkedHashMap<>(cache);
    }

    public int size() {
        return cache.size();
    }

    private void validateOperator(CognitionOperator operator, Set<String> seenIds) {
        if (operator.operatorId() == null || operator.operatorId().isBlank()) {
            throw new IllegalArgumentException("operatorId 为空");
        }

        if (operator.category() == null) {
            throw new IllegalArgumentException("category 为空: " + operator.operatorId());
        }

        try {
            DimensionCategory.valueOf(operator.category().name());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("category 不在 DimensionCategory 枚举中: "
                    + operator.operatorId() + " -> " + operator.category());
        }

        if (seenIds.contains(operator.operatorId())) {
            throw new IllegalArgumentException("operatorId 重复: " + operator.operatorId());
        }
    }
}
