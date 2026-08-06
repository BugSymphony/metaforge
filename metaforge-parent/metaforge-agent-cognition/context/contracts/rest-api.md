---
id: rest-api
protocol: REST
version: 1.1.0
owner: metaforge-agent-cognition
description: Execute a cognitive query using the specified template.
type: business
---

# REST API Contract

## Overview

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

## Cognitive Perspectives

The unified query engine supports **14 built-in cognitive perspectives**. Each perspective is a self-contained cognitive query unit that queries upstream BCs and returns a structured section. The `perspectives` parameter in the request body can be used to select which perspectives to execute (empty = engine decides by template); the `cognition_depth` and `agent_archetype` parameters control how many perspectives are activated and their priority order.

Perspective scope semantics:
- **BUNDLE**: perspective only applies in `BUNDLE_LEVEL` context (queried against a whole Bundle)
- **ENTITY**: perspective only applies in `ENTITY_LEVEL` context (filtered by `entity_fqn`)
- **BOTH**: applies in both contexts (in ENTITY_LEVEL mode, filtered by `entity_fqn` graph edges)

| Perspective ID | Scope | 功能 | 用途 |
|----------------|-------|------|------|
| `entity_profile` | BOTH | 通过 FQN 精准查询获取实体的完整 M1 实例内容，并通过元模型获取所属 EntitySchema 的结构定义用于字段语义解释 | 获取指定实体的完整画像（全属性、Schema 结构说明、历史版本信息），是实体级认知的基础视角 |
| `domain_location` | ENTITY | 从 `entity_fqn` 出发沿 COMPOSITION 入边反向追溯该实体在 L1-L5 业务知识树中的完整归属路径 | 明确某实体在业务领域层级中的具体位置，未接入分类树时返回空路径标注 |
| `composition_tree` | ENTITY | 递归展开指定实体的 COMPOSITION 树（FORWARD 子树 / BACKWARD 父链 / BOTH 双向） | 查看实体"由什么组成"或"被谁包含"的完整组成结构 |
| `relationship_graph` | ENTITY | 以 `entity_fqn` 为中心展开关系邻域（默认 1-3 度），结果按 AssociationType 分组 | 查看实体关联的语义关系网络，每组列出关联远端实体及关系语义说明 |
| `constraint_set` | ENTITY | 以 `entity_fqn` 为端点沿 DEPENDENCY_INFLUENCE 入边和 ASSOCIATION_REFERENCE 边查询约束实体，并提取 EntitySchema 的 JSON Schema 硬边界（必填字段、枚举值、取值范围） | 获取执行某实体时必须满足的约束与边界规则，按 constraint_level（MANDATORY/RECOMMENDED/REFERENCE）分级 |
| `capability_catalog` | ENTITY | 以 `entity_fqn` 为端点获取能力实体 FQN 列表、能力实体详情，并自动展开 protocol 子类型详情 | 了解该实体对外暴露的操作能力清单（可调用 API/能力） |
| `flow_blueprint` | BUNDLE | 将 PROCESS_SEQUENCE 关系拓扑构建为有序步骤序列，含 branch_points、entry_step、exit_steps | 查看整个 Bundle 的流程蓝图（步骤序列），用于流程级认知 |
| `decision_matrix` | BOTH | 查询 `entity_fqn` 的 PROCESS_SEQUENCE 出边（出边数 >1 为决策点），评估每个选项的下游影响 | 识别流程中的决策分支点，评估各分支影响，辅助决策判断 |
| `impact_trace` | ENTITY | 以 `entity_fqn` 为起点执行正向影响扩散（深度 3）、反向依赖溯源、影响路径详情 | 评估变更某个实体的影响范围与依赖关系，供变更影响分析使用 |
| `prerequisite_chain` | ENTITY | 从 `entity_fqn` 出发沿 DEPENDENCY_INFLUENCE 入边反向追溯前置依赖链，按层级展开依赖树 | 获取执行某实体前必须满足的前置依赖条件链 |
| `domain_navigation` | BUNDLE | 沿 L1 SubjectDomainGroup → L2 SubjectDomain → Task 逐层下钻，默认懒加载（返回当前层子节点概要 + has_more），expand=all 时全量展开 | 渐进式浏览平台的领域结构，首屏导航入口 |
| `instance_catalog` | BOTH | 按 entityTypes 过滤交付指定 Bundle 的 M1 实例及其关系清单（ENTITY 模式按 entity_fqn 做关系边过滤） | 查看指定 Bundle 下满足条件的实例列表，供枚举与筛选 |
| `bundle_directory` | BUNDLE | 交付平台已发布 Bundle 实例列表及其已填充的主题域树（L1→L2→Task） | 查看平台当前所有已发布 Bundle 及其主题域目录总览 |
| `schema_inventory` | BUNDLE | 枚举指定 Bundle 下已发布的所有 EntitySchema FQN 和名称，以及每类 Schema 的 M1 实例数量统计（无实例时保留条目并标注 count=0） | 查看 Bundle 的元模型结构清单与各结构实例规模 |

## Built-in Templates

The endpoint routes via `templateId` to a pre-configured template in `cognition-templates.yml`. Each template declares a fixed perspective combination, depth trim level, and agent archetype adaptation. Templates are declarative — new business scenarios only require declaring a new template config, no code changes.

| Template ID | 功能 | 用途 | 视角组合 | 深度裁剪 | 代理原型 |
|-------------|------|------|----------|----------|----------|
| `bundle-catalog` | Bundle 目录与版本概览 | 消费端首屏接入——查看平台已发布的 Bundle 列表并进入主题域导航 | `bundle_directory` + `domain_navigation` | L1 | exploration |
| `cognition-guidance` | 完整认知引导（14 个视角全量） | 深度认知查询引擎——接受 `perspectives` 参数按需动态组合任意视角集合，跨 Bundle 通用 | 全部 14 个视角（可裁剪） | L1/L2/L3 | 可配置 |
| `task-brief` | 一站式任务摘要 | 代理执行任务前获取完整的执行上下文简报（10 视角覆盖实体、关系、约束、能力、流程、决策、影响） | `entity_profile`、`domain_location`、`composition_tree`、`relationship_graph`、`constraint_set`、`capability_catalog`、`flow_blueprint`、`decision_matrix`、`impact_trace`、`prerequisite_chain` | L2 | execution |
| `step-guide` | 实体即时指导 | 对指定实体（entity_fqn）做实体级过滤的即时操作指导，附加 FQN 归属校验和 adjacent_context 局部导航 | `entity_profile`、`constraint_set`、`capability_catalog`、`decision_matrix`、`impact_trace`、`relationship_graph` | L2 | execution |
| `navigate` | 渐进式领域导航 | 按需逐层下钻浏览领域结构（支持 anchor_fqn、level、cursor、page_size、expand 参数），懒加载控制单次数据量 | `domain_navigation` | L1 | exploration |

Template routing rules:
- Passing an unregistered `templateId` returns error code `34001 TEMPLATE_NOT_FOUND`.
- `task-brief` and `step-guide` are stateless and idempotent — repeated identical calls return identical results (except query timestamps).
- The template depth trim is overridable per request via the `cognition_depth` request parameter.

## Response Schema

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

## Error Codes

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
