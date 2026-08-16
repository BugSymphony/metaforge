package com.metaforge.agent.cognition.api.dto.response;

import com.metaforge.agent.cognition.api.enums.OutputFormat;
import com.metaforge.agent.cognition.api.spi.CognitionResult;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 认知查询响应——顶层结构：template / contextMeta / dimensions(扁平算子列表) / format / updatedScope。
 */
public record CognitionResponse(
        String template,
        ContextMeta contextMeta,
        List<CognitionResult> dimensions,
        OutputFormat format,
        Map<String, Object> updatedScope
) {

    public static CognitionResponse json(String templateId, ContextMeta contextMeta,
                                          List<CognitionResult> dimensions,
                                          Map<String, Object> updatedScope) {
        return new CognitionResponse(templateId, contextMeta, dimensions, OutputFormat.JSON, updatedScope);
    }

    public static CognitionResponse prompt(String templateId, ContextMeta contextMeta,
                                            List<CognitionResult> dimensions,
                                            Map<String, Object> updatedScope) {
        return new CognitionResponse(templateId, contextMeta, dimensions, OutputFormat.PROMPT, updatedScope);
    }

    public CognitionResponse {
        dimensions = dimensions != null ? dimensions : Collections.emptyList();
    }
}
