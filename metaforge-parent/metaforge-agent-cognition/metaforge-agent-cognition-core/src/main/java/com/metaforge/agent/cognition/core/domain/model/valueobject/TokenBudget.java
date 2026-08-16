package com.metaforge.agent.cognition.core.domain.model.valueobject;

import com.metaforge.agent.cognition.api.enums.CognitionDepth;

public record TokenBudget(int maxTokens, int estimated) {

    private static final int AUTO_DOWNGRADE_THRESHOLD = 500;

    public TokenBudget {
        if (maxTokens < 1) {
            throw new IllegalArgumentException("maxTokens 必须 >= 1");
        }
        if (estimated < 0) {
            estimated = 0;
        }
    }

    public TokenBudget(int maxTokens) {
        this(maxTokens, maxTokens);
    }

    public CognitionDepth autoDowngrade() {
        return maxTokens < AUTO_DOWNGRADE_THRESHOLD ? CognitionDepth.L1 : null;
    }

    @Override
    public String toString() {
        return estimated + "/" + maxTokens;
    }
}
