package com.metaforge.agent.cognition.api.service;

import com.metaforge.agent.cognition.api.dto.request.CognitionRequest;
import com.metaforge.agent.cognition.api.dto.response.CognitionResponse;

/**
 * 统一认知查询服务接口，单一入口执行认知模板并返回结构化认知结果。
 *
 * <p>执行流程：
 * <ol>
 *   <li>模板解析 — 从 TemplateRegistry 按 templateId 解析 TemplateDefinition</li>
 *   <li>Scope 校验 — 校验请求 scope 的有效性，若 required 则强制要求</li>
 *   <li>Archetype 过滤 — 按请求 agentArchetype 过滤模板算子清单</li>
 *   <li>算子编排 — 按 priority 排序后依次执行各算子</li>
 *   <li>深度裁剪 — 按 cognitionDepth 裁剪非强制算子结果</li>
 *   <li>输出组装 — 按 DimensionCategory 分组算子结果，聚合 updated_scope</li>
 *   <li>ContextMeta 生成 — 组装版本锚、scope、Token 估算等元信息</li>
 * </ol>
 *
 * <h3>错误场景</h3>
 * <ul>
 *   <li>34001: TEMPLATE_NOT_FOUND — templateId 未在 TemplateRegistry 中注册</li>
 *   <li>34003: INVALID_SCOPE — scope 中声明的 bundle/package/entitySchema 无效</li>
 *   <li>34004: ENTITY_OUT_OF_SCOPE — entityFqn 不在声明的 scope 范围内</li>
 *   <li>34005: MISSING_SCOPE — 模板强制要求 scope 但请求未提供</li>
 *   <li>34006: OPERATOR_EXECUTION_ERROR — 算子执行异常</li>
 *   <li>34007: OPERATOR_TIMEOUT — 算子执行超时</li>
 *   <li>34008: UPSTREAM_UNAVAILABLE — 上游 BC 服务不可用</li>
 *   <li>34012: ARCHETYPE_NOT_SUPPORTED — agentArchetype 不被模板任一算子支持</li>
 * </ul>
 *
 * @param templateId 认知模板标识（如 DISCOVER, ORIENT, BRIEF, GUIDE, FORECAST, DELEGATE）
 * @param request 认知查询请求参数
 * @return 结构化认知响应（含 contextMeta、dimensions、format、updatedScope）
 */
public interface CognitionQueryService {

    CognitionResponse execute(String templateId, CognitionRequest request);
}
