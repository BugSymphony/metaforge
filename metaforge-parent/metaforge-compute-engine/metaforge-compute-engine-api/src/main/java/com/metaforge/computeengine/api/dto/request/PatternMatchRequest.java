package com.metaforge.computeengine.api.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

/**
 * 图模式匹配请求。
 *
 * @param pattern    模式字符串，如 "* -[?]-> * -[?]-> *"
 * @param maxResults 最大匹配结果数
 * @author metaforge
 */
public record PatternMatchRequest(
        @NotBlank String pattern,
        int maxResults
) implements Serializable {
}
