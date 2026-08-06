package com.metaforge.graph.domain.service;

import com.metaforge.graph.api.constant.GraphErrorCode;
import com.metaforge.graph.infrastructure.config.GraphBizException;
import com.metaforge.graph.domain.model.valueobject.CardinalityRule;
import com.metaforge.graph.domain.repository.RelationInstanceRepository;
import org.springframework.stereotype.Component;

/**
 * 基数约束校验领域服务。
 * 查询主表已存在的同类关系数量并与 CardinalityRule 比对。
 */
@Component
public class CardinalityValidationService {

    private final RelationInstanceRepository relationInstanceRepository;

    public CardinalityValidationService(RelationInstanceRepository relationInstanceRepository) {
        this.relationInstanceRepository = relationInstanceRepository;
    }

    /**
     * 校验关系基数是否在约束范围内。
     *
     * @param relationType     关系类型
     * @param sourceEntityFqn  源端实体 FQN
     * @param targetEntityFqn  目标端实体 FQN
     * @param rule             基数约束规则
     * @param excludeFqn       排除的 FQN（更新场景，null 表示新增）
     */
    public void validate(String relationType, String sourceEntityFqn, String targetEntityFqn,
                         CardinalityRule rule, String excludeFqn) {
        long sourceCount = relationInstanceRepository.countByRelationTypeAndSourceEntityFqn(relationType, sourceEntityFqn);
        if (excludeFqn != null) {
            sourceCount = Math.max(0, sourceCount - 1);
        }

        if (isOneSide(rule.getSourceCardinality())) {
            if (sourceCount >= 1) {
                throw new CardinalityViolationException(
                        "源端基数超限: " + sourceEntityFqn + " 已有 " + sourceCount + " 条同类关系，约束上限为 1");
            }
        }

        if (isOneSide(rule.getTargetCardinality())) {
            long targetCount = relationInstanceRepository.countByRelationTypeAndTargetEntityFqn(relationType, targetEntityFqn);
            if (excludeFqn != null) {
                targetCount = Math.max(0, targetCount - 1);
            }
            if (targetCount >= 1) {
                throw new CardinalityViolationException(
                        "目标端基数超限: " + targetEntityFqn + " 已有 " + targetCount + " 条同类关系，约束上限为 1");
            }
        }
    }

    private boolean isOneSide(String cardinality) {
        return cardinality != null && cardinality.trim().equals("1");
    }

    public static class CardinalityViolationException extends GraphBizException {
        public CardinalityViolationException(String message) {
            super(GraphErrorCode.CARDINALITY_EXCEEDED, message);
        }

        @Override
        public String getErrorCodeName() {
            return "CARDINALITY_EXCEEDED";
        }
    }
}
