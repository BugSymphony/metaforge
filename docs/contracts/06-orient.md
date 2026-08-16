# ORIENT 模板契约文档 —— 业务域定位

> Agent 需要在业务域内定位关心的主题域、业务对象、可执行任务、Agent。支持任意 Bundle 定义的层级结构，支持跨 Bundle 下钻。每次返回一层 lazy 子节点，按 entity_type 分组。

- **templateId**: `ORIENT`
- **入口**: `POST http://localhost:8080/api/v1/cognition/ORIENT`
- **公共约定**: 见 [00-common.md](./00-common.md)

---

## 1. 模板定位

ORIENT 是"业务域定位/导航"服务。它沿主题域树（L1-L5）+ 可执行实体（Task/Agent）**逐层下钻**，每次返回一层的子节点，按 `entity_type` 分组。帮助 Agent 回答"我该在哪个域、哪个业务对象、哪个任务/Agent 上操作"。

**层级模型（模板 config.global.levelAliases）**：

| 别名 | EntitySchema | 含义 |
|------|--------------|------|
| L1 | `common.SubjectDomainGroup` | 主题域分组 |
| L2 | `common.SubjectDomain` | 主题域 |
| L3 | `common.BusinessObject` | 业务对象 |
| L4 | `common.LogicalEntity` | 逻辑实体 |
| L5 | `common.Attribute` | 属性 |
| Task | `agent.Task` | 可执行任务 |
| Agent | `agent.Agent` | Agent |

**适用场景**
- 定位业务主题域/业务对象。
- 下钻到可执行任务/Agent 层面，为后续 BRIEF/DELEGATE 提供锚点。
- 渐进式浏览平台业务语义组织。

---

## 2. 请求

### 2.1 请求体

```json
{
  "scope": { "bundles": ["metaforge:1.0.0"], "packages": [], "domainGroups": [], "domains": [], "entitySchemas": [] },
  "params": {
    "parent_fqn": "metaforge:1.0.0",
    "level": null,
    "cursor": 0,
    "page_size": 20
  },
  "format": "JSON",
  "cognitionDepth": "L3",
  "agentArchetype": "EXECUTION",
  "maxTokens": 8000
}
```

### 2.2 params 参数

| 字段 | 类型 | 必填 | 默认 | 说明 |
|------|------|------|------|------|
| `selectOperators` | array | 否 | `[]` | 算子子集（当前仅 `ontological.domain-drilldown`） |
| `parent_fqn` | string | 否 | null | 要展开的父节点实例 FQN；`null`=返回 scope 下顶层节点 |
| `level` | string | 否 | null | 过滤层级：`null`=自动发现（按 entity_type 分组）；`L1`-`L5`=便捷别名；`Task`/`Agent`=类型别名；任意 EntitySchemaFQN=精确过滤 |
| `cursor` | integer | 否 | 0 | 分页页码（1-based） |
| `page_size` | integer | 否 | 20 | 每页数量（1-100） |

**level 非法值** → **34013 INVALID_LEVEL**。

### 2.3 scopeBehavior

| 项 | 值 | 说明 |
|----|----|------|
| acceptsScope | true | 接受可选 scope |
| scopeRequired | false | 不强制 scope |
| producesUpdatedScope | false | 不产出更新 scope |
| scopeFields | `[bundles, domain_groups, domains, entity_schemas]` | 4 个维度参与过滤 |

---

## 3. 响应

### 3.1 响应体

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "template": "ORIENT",
    "contextMeta": { "template": "ORIENT", "scopeApplied": { }, "tokenEstimate": 8000 },
    "dimensions": [
      { "operatorId": "ontological.domain-drilldown", "name": "领域下钻", "category": "ONTOLOGICAL", "data": { }, "success": true }
    ],
    "format": "JSON",
    "updatedScope": null
  },
  "traceId": "..."
}
```

### 3.2 算子列表

| operatorId | 名称 | 优先级 | 是否强制 | 适用 archetype |
|-----------|------|--------|----------|----------------|
| ontological.domain-drilldown | 领域下钻 | 100 | **是** | execution, exploration |

**输入 config**（模板固定）：`relationTypes: [COMPOSITION]`（沿 COMPOSITION 关系下钻）。

---

## 4. 算子详解

### 4.1 ontological.domain-drilldown（领域下钻）— 必选

**功能**：沿 COMPOSITION 关系逐层下钻，返回当前父节点的子节点，按 `entity_type` 分组。支持：
- 自动发现层级（level=null，按子节点实际 entity_type 分组）
- 类型别名解析（L1-L5 / Task / Agent）
- 精确 EntitySchema 过滤
- 跨 Bundle 下钻

**适用场景**：业务域导航。

**输出 data**：

```json
{
  "children_grouped": {
    "metaforge:1.0.0.common.SubjectDomain": [
      { "fqn": "metaforge:1.0.0.common.Domain_Inventory", "name": "库存管理域", "entitySchemaFqn": "metaforge:1.0.0.common.SubjectDomain" }
    ]
  },
  "level": "metaforge:1.0.0.common.SubjectDomain"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `children_grouped` | object | 子节点按 `entity_type`（EntitySchemaFQN）分组的映射；每组为节点摘要列表 `{fqn, name, entitySchemaFqn, ...}` |
| `level` | string | 当前解析/返回的层级（EntitySchemaFQN 或别名） |

> 使用方式：每次拿 `parent_fqn` 下钻一层；把返回的某个子节点 `fqn` 作为下一次的 `parent_fqn` 继续下钻。

---

## 5. 错误场景

| 场景 | 结果 |
|------|------|
| `level` 无法解析为有效 EntitySchema 类型 | **34013 INVALID_LEVEL** |
| `selectOperators` 含模板未声明算子 | 34014 |
| scope 声明无效 | 34003 |

---

## 6. 完整示例

```bash
# 顶层自动发现（按 entity_type 分组）
curl -s -X POST http://localhost:8080/api/v1/cognition/ORIENT \
  -H 'Content-Type: application/json' \
  -d '{
    "scope": {"bundles":["metaforge:1.0.0"],"packages":[],"domainGroups":[],"domains":[],"entitySchemas":[]},
    "params": {"parent_fqn":"metaforge:1.0.0"},
    "format":"JSON","cognitionDepth":"L3","agentArchetype":"EXECUTION","maxTokens":8000
  }'

# 下钻到域层，按 Task 过滤
curl -s -X POST http://localhost:8080/api/v1/cognition/ORIENT \
  -H 'Content-Type: application/json' \
  -d '{
    "scope": {"bundles":["metaforge:1.0.0"],"packages":[],"domainGroups":[],"domains":[],"entitySchemas":[]},
    "params": {"parent_fqn":"metaforge:1.0.0.common.Domain_Inventory","level":"Task"},
    "format":"JSON","cognitionDepth":"L3","agentArchetype":"EXECUTION","maxTokens":8000
  }'
```
