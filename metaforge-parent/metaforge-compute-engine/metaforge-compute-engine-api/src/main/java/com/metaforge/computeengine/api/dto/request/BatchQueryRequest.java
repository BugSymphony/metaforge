package com.metaforge.computeengine.api.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.io.Serializable;
import java.util.List;

/**
 * 批量语义查询请求。
 *
 * @param fqns FQN 列表（上限 200）
 * @author metaforge
 */
public record BatchQueryRequest(
        @NotEmpty List<String> fqns
) implements Serializable {
}
