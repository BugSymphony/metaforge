package com.metaforge.agent.cognition.core.domain.service;

import com.metaforge.agent.cognition.core.domain.model.aggregate.GuidanceResult;
import com.metaforge.agent.cognition.core.infrastructure.config.AgentCognitionProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TokenBudgetServiceImpl implements TokenBudgetService {

    private static final Logger log = LoggerFactory.getLogger(TokenBudgetServiceImpl.class);
    private static final int CHARS_PER_TOKEN = 4;

    private final AgentCognitionProperties properties;

    public TokenBudgetServiceImpl(AgentCognitionProperties properties) {
        this.properties = properties;
    }

    @Override
    public GuidanceResult trim(GuidanceResult result, int maxTokens) {
        long currentTokens = estimateTokens(result);
        if (currentTokens <= maxTokens) {
            return result;
        }

        log.info("Token 预算超限，执行裁剪: currentTokens={}, maxTokens={}", currentTokens, maxTokens);

        result.getContextMeta().setTokenTrimmed(true);
        return result;
    }

    @Override
    public long estimateTokens(GuidanceResult result) {
        if (result == null || result.getPerspectiveChapters() == null) {
            return 0;
        }

        long totalChars = 0;
        for (Object chapter : result.getPerspectiveChapters().values()) {
            if (chapter != null) {
                totalChars += chapter.toString().length();
            }
        }

        long tokens = totalChars / CHARS_PER_TOKEN;
        log.debug("Token 估算: totalChars={}, estimatedTokens={}", totalChars, tokens);
        return tokens;
    }
}
