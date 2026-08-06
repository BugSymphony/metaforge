package com.metaforge.agent.cognition.core.domain.service;

import com.metaforge.agent.cognition.core.domain.model.aggregate.GuidanceResult;

public interface TokenBudgetService {

    GuidanceResult trim(GuidanceResult result, int maxTokens);

    long estimateTokens(GuidanceResult result);
}
