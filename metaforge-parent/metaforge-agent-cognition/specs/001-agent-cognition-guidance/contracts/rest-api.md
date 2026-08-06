# REST API Contract

## Base URL

```
/api/v1/cognition
```

## Endpoints

### POST /api/v1/cognition/{templateId}

Execute a cognitive query using the specified template.

**Path Parameters**

| Parameter    | Type   | Required | Description              |
|-------------|--------|----------|--------------------------|
| templateId  | string | Y        | Template identifier       |

**Request Body**

Content-Type: `application/json`

```json
{
  "bundle_fqns": ["order:1.0.0", "refund:1.0.0"],
  "entity_fqn": "order:1.0.0.Step_CheckInventory",
  "entity_types": ["order:1.0.0.ExecutionRule"],
  "subject_domain_fqn": "order:1.0.0.SubjectDomain_Order",
  "scope_mode": "INHERITED",
  "cognition_depth": "L2",
  "agent_archetype": "execution",
  "max_tokens": 8000,
  "expand": "lazy",
  "format": "json",
  "context_parameters": {"source": "opencode-agent"},
  "cursor": 0,
  "page_size": 20
}
```

**Request Field Descriptions**

| Field                | Type            | Required | Default      | Description                                                                 |
|----------------------|-----------------|----------|--------------|-----------------------------------------------------------------------------|
| bundle_fqns          | string[]        | Y        | —            | Bundle FQN 列表                                                               |
| entity_fqn           | string          | N        | —            | 实体 FQN，ENTITY_LEVEL 模式时必填                                                  |
| entity_types         | string[]        | N        | —            | 实体类型过滤列表                                                                   |
| subject_domain_fqn   | string          | N        | —            | 主体域 FQN                                                                     |
| scope_mode           | string          | N        | "INHERITED"  | 作用域模式：ENTITY_LEVEL / PACKAGE / BUNDLE / INHERITED                           |
| cognition_depth      | string          | N        | "L2"         | 认知深度：L1 / L2 / L3                                                           |
| agent_archetype      | string          | N        | "execution"  | 代理原型：execution / exploration / audit / orchestration                         |
| max_tokens           | number          | N        | 8000         | 最大 Token 数                                                                   |
| expand               | string          | N        | "lazy"       | 展开模式：eager / lazy                                                            |
| format               | string          | N        | "json"       | 输出格式：json / prompt                                                           |
| context_parameters   | object          | N        | {}           | 上下文参数，透传键值对                                                                 |
| cursor               | number          | N        | 0            | 分页游标                                                                        |
| page_size             | number          | N        | 20           | 分页大小                                                                        |

## Built-in Template IDs

| Template ID        | Description                                    |
|--------------------|------------------------------------------------|
| bundle-catalog     | Bundle 目录与版本概览                               |
| cognition-guidance | 完整认知引导（14 个视角全量）                             |
| task-brief         | 任务摘要（entity_profile + constraint_set）         |
| step-guide         | 步骤引导（entity_profile + capability_catalog）     |
| navigate           | 导航视图（entity_profile + domain_navigation）      |

## Responses

### JSON Format (`format: "json"`)

**200 OK**

```json
{
  "context_meta": {
    "template_id": "cognition-guidance",
    "context_mode": "ENTITY_LEVEL",
    "data_version_anchors": {
      "order": {"version": "1.0.0", "queriedAt": "2026-08-01T10:30:00Z"}
    },
    "truncated_perspectives": [
      {"perspective_id": "impact_trace", "truncated": true, "reason": "TIMEOUT"}
    ],
    "skipped_perspectives": [
      {"perspective_id": "domain_navigation", "reason": "BUNDLE scope skipped in ENTITY_LEVEL"}
    ]
  },
  "perspectives": {
    "entity_profile": {
      "perspective_id": "entity_profile",
      "status": "OK",
      "data": { ... },
      "truncated": false,
      "truncated_reason": null,
      "error_message": null
    },
    "constraint_set": {
      "perspective_id": "constraint_set",
      "status": "OK",
      "data": { ... },
      "truncated": false,
      "truncated_reason": null,
      "error_message": null
    },
    "capability_catalog": {
      "perspective_id": "capability_catalog",
      "status": "OK",
      "data": { ... },
      "truncated": false,
      "truncated_reason": null,
      "error_message": null
    }
  }
}
```

### Prompt Format (`format: "prompt"`)

**200 OK** — Content-Type: `text/markdown`

The prompt format renders the same semantic content as a Markdown document suitable for direct LLM injection.  Each perspective is rendered as a Markdown section with its data structured for readability by LLM contexts.

Example:

```markdown
# Cognition Guidance Report
**Template**: cognition-guidance
**Mode**: ENTITY_LEVEL

## Data Version Anchors
- `order`: 1.0.0 (queried 2026-08-01T10:30:00Z)

## Entity Profile
...

## Constraint Set
...

## Capability Catalog
...
```

### Error Response

Uses foundation-core `ApiResponse` format.

```json
{
  "code": 34004,
  "message": "实体FQN不属于任何已发布Bundle",
  "data": {
    "candidates": ["order:1.0.0.Step_CheckInventory"]
  },
  "traceId": "a1b2c3d4e5f6"
}
```

### HTTP Status Codes

| HTTP Status | Error Code            | Condition                                  |
|-------------|-----------------------|--------------------------------------------|
| 404         | 34001                 | templateId not registered                   |
| 400         | 34002                 | bundleFqn format invalid                    |
| 400         | 34003                 | bundleFqns list empty                       |
| 400         | 34004                 | entityFqn prefix not in any Bundle          |
| 500         | 34005                 | Single perspective timed out (200ms)        |
| 502         | 34006                 | Upstream BC unavailable                     |

## Headers

| Header              | Value                  | Description                       |
|---------------------|------------------------|-----------------------------------|
| Content-Type        | application/json       | Request body media type            |
| Accept              | application/json       | Response media type (JSON)         |
| Accept              | text/markdown          | Response media type (prompt)       |
| X-Trace-Id          | string                 | Distributed tracing ID             |
