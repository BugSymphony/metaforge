---
id: agent-cognition.rest-api
protocol: REST
version: 1.0.0
owner: metaforge-agent-cognition
description: 认知引擎 REST API 接口契约。提供统一模板驱动的认知查询 HTTP 端点。
type: business
---

# REST API Contract: metaforge-agent-cognition

**Protocol**: RESTful HTTP/1.1 + JSON
**Base URL**: `/api/v1`
**Version**: 1.0.0

> 所有 REST 端点复用 foundation-core 的 `ApiResponse<T>` 统一响应格式（code/message/data/traceId）。
> 响应体在成功时被 foundation 全局切面自动包装为 `ApiResponse.data`。

---

## Overview

按模板 ID 执行一次认知查询，编排认知算子、裁剪结果、格式化输出。该端点是整个认知引擎的唯一对外入口，所有消费方通过该端点获取认知能力。

---

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/cognition/{templateId}` | 统一认知查询执行入口 |

---

## Request Parameters

### POST /api/v1/cognition/{templateId}

**描述**: 按模板 ID 执行一次认知查询，编排认知算子、裁剪结果、格式化输出。

**路径参数**:

| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| templateId | string | 是 | 模板唯一标识（DISCOVER/ORIENT/BRIEF/GUIDE/FORECAST/DELEGATE） |

### Request Body

```json
{
  "scope": {
    "bundles": ["order:1.0.0"],
    "packages": ["order:1.0.0.pkg_order"],
    "domain_groups": [],
    "domains": [],
    "entity_schemas": []
  },
  "params": {
    "parent_fqn": "",
    "entity_fqn": "order:1.0.0.pkg_order.Order_001"
  },
  "format": "json",
  "cognition_depth": "L2",
  "agent_archetype": "execution",
  "max_tokens": 8000
}
```

**请求字段**:

| 字段 | 类型 | 必填 | 默认值 | 描述 |
|------|------|------|--------|------|
| scope | object | 条件 | null | 认知边界五字段。scopeRequired=true 时必填 |
| scope.bundles | string[] | 否 | — | Bundle FQN 白名单 |
| scope.packages | string[] | 否 | — | Package FQN 白名单 |
| scope.domain_groups | string[] | 否 | — | 域组 FQN 白名单 |
| scope.domains | string[] | 否 | — | 域 FQN 白名单 |
| scope.entity_schemas | string[] | 否 | — | EntitySchema FQN 白名单 |
| params | object | 否 | `{}` | 模板专用参数，由模板 inputSchema 定义 |
| format | string | 否 | `"json"` | 输出格式：`"json"` 或 `"prompt"` |
| cognition_depth | string | 否 | `"L2"` | 认知深度：`"L1"`/`"L2"`/`"L3"` |
| agent_archetype | string | 否 | `"execution"` | Agent 原型：`"execution"`/`"exploration"`/`"audit"`/`"orchestration"` |
| max_tokens | integer | 否 | 8000 | 最大 Token 预算；`< 500` 自动降为 L1 |

---

## Response Schema

### Response (Success — format: "json")

**HTTP Status**: 200 OK

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "template": "DISCOVER",
    "context_meta": {
      "template": "DISCOVER",
      "version_anchors": {
        "order": "order:1.0.0",
        "payment": "payment:1.2.0"
      },
      "scope_applied": {
        "bundles": ["order:1.0.0"],
        "packages": null,
        "domain_groups": null,
        "domains": null,
        "entity_schemas": null
      },
      "token_estimate": 1200,
      "generated_at": "2026-08-11T10:30:00Z",
      "skipped_entities": [],
      "truncated_perspectives": ["procedural"]
    },
    "dimensions": {
      "ontological": {
        "bundle_discovery": { "bundles": [...] },
        "package_explorer": { "packages": [...] }
      },
      "structural": {
        "decomposition": { "tree": {...} }
      },
      "relational": {
        "direct_link": { "relations": [...] }
      }
    },
    "format": "json",
    "content": null
  },
  "traceId": "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6"
}
```

### Response (Success — format: "prompt")

**HTTP Status**: 200 OK

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "template": "BRIEF",
    "context_meta": {
      "template": "BRIEF",
      "version_anchors": { "order": "order:1.0.0" },
      "scope_applied": { "bundles": ["order:1.0.0"], ... },
      "token_estimate": 450,
      "generated_at": "2026-08-11T10:30:00Z",
      "skipped_entities": [],
      "truncated_perspectives": []
    },
    "dimensions": null,
    "format": "prompt",
    "content": "# BRIEF 认知简报\n\n## 上下文元信息\n\n- **模板**: BRIEF\n- **数据版本**: order@1.0.0\n- **生成时间**: 2026-08-11T10:30:00Z\n- **Token 估算**: 450\n\n## 本体论\n...\n"
  },
  "traceId": "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6"
}
```

### Response (Error)

**HTTP Status**: 4xx/5xx

```json
{
  "code": 34001,
  "message": "模板 'UNKNOWN' 未注册",
  "data": null,
  "traceId": "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6"
}
```

---

## Error Codes

| 错误码 | HTTP Status | 场景 |
|--------|------------|------|
| 34001 | 404 | templateId 未注册 |
| 34002 | 422 | 模板配置校验失败 |
| 34003 | 400 | scope 中 bundle/package FQN 无效 |
| 34004 | 403 | 实体超出 scope 边界 |
| 34005 | 400 | scopeRequired 模板缺少 scope |
| 34006 | 422 | 模板引用未注册的算子 |
| 34007 | 422 | 算子引用无法解析 |
| 34008 | 504 | required 算子执行超时 |
| 34009 | 500 | required 算子执行失败 |
| 34010 | 400 | format 参数无效 |
| 34011 | 502 | 上游 BC 不可用 |
| 34012 | 400 | archetype 无算子配置 |

---

## OpenAPI 标签

所有端点统一使用 Swagger 分组标签：

```java
@Tag(name = "agent-cognition")
```

禁止自定义 `springdoc.*` 配置或 `OpenAPI` bean。

---

## 调用示例

```bash
# JSON 格式 DISCOVER 查询
curl -X POST http://localhost:8080/api/v1/cognition/DISCOVER \
  -H "Content-Type: application/json" \
  -H "Accept-Language: zh-CN" \
  -d '{
    "scope": { "bundles": ["order:1.0.0"] },
    "params": { "parent_fqn": "" },
    "format": "json",
    "cognition_depth": "L2",
    "agent_archetype": "execution",
    "max_tokens": 8000
  }'

# Prompt 格式 BRIEF 查询
curl -X POST http://localhost:8080/api/v1/cognition/BRIEF \
  -H "Content-Type: application/json" \
  -d '{
    "scope": { "bundles": ["order:1.0.0"] },
    "params": { "entity_fqn": "order:1.0.0.pkg_order.Order_001" },
    "format": "prompt",
    "cognition_depth": "L3",
    "agent_archetype": "execution"
  }'
```
