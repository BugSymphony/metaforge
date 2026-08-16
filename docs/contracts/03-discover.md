# DISCOVER 模板契约文档 —— 元模型发现

> 探索平台元模型结构——发现 Bundle、Package、EntitySchema、RelationSchema。通过 operators 参数选择要展开的一层，渐进式下钻建立对平台语义组织的全局认知。

- **templateId**: `DISCOVER`
- **入口**: `POST http://localhost:8080/api/v1/cognition/DISCOVER`
- **公共约定**: 见 [00-common.md](./00-common.md)

---

## 1. 模板定位

DISCOVER 是"平台元模型浏览"服务。它回答"平台上有什么"：
- 有哪些语义包（Bundle）
- 包内命名空间树（Package）
- 有哪些实体类型（EntitySchema）与实例数量
- 有哪些关系类型（RelationSchema）

通过 `selectOperators` 选择要展开的层，逐层下钻建立对平台语义组织的全局认知。**注意：本模板的多个算子 `data` 是实体列表（array）而非对象。**

**适用场景**
- 接入新平台时盘点元模型（有哪些 Bundle/Package/EntitySchema/RelationSchema）。
- 确认某类实体/关系是否存在及其定义。
- 渐进式下钻：Bundle → Package → EntitySchema → 实例。

---

## 2. 请求

### 2.1 请求体

```json
{
  "scope": { "bundles": ["metaforge:1.0.0"], "packages": [], "domainGroups": [], "domains": [], "entitySchemas": [] },
  "params": {
    "parent_fqn": "metaforge:1.0.0.agent",
    "selectOperators": ["ontological.entity-schema-inventory"],
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
| `selectOperators` | array | 否 | `[]` | 算子子集。如 `["ontological.bundle-discovery"]` 仅展开 Bundle 层 |
| `parent_fqn` | string | 否 | null | 要展开的父节点 FQN 锚点；`null`=返回全平台 Bundle 列表 |
| `cursor` | integer | 否 | 0 | 分页页码（1-based） |
| `page_size` | integer | 否 | 20 | 每页数量（1-100） |

### 2.3 scopeBehavior

| 项 | 值 | 说明 |
|----|----|------|
| acceptsScope | true | 接受可选 scope |
| scopeRequired | false | 不强制 scope |
| producesUpdatedScope | false | 不产出更新 scope |
| scopeFields | `[bundles, packages, entity_schemas]` | 仅这 3 个维度参与过滤 |

---

## 3. 响应

### 3.1 响应体

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "template": "DISCOVER",
    "contextMeta": { "template": "DISCOVER", "scopeApplied": { }, "tokenEstimate": 8000 },
    "dimensions": [
      { "operatorId": "ontological.bundle-discovery", "name": "Bundle 发现", "category": "ONTOLOGICAL", "data": [ ... ], "success": true },
      { "operatorId": "ontological.package-explorer", "name": "Package 探索", "category": "ONTOLOGICAL", "data": [ ... ], "success": true },
      { "operatorId": "ontological.entity-schema-inventory", "name": "实体类型盘点", "category": "ONTOLOGICAL", "data": [ ... ], "success": true },
      { "operatorId": "ontological.relation-schema-inventory", "name": "关系类型盘点", "category": "ONTOLOGICAL", "data": [ ... ], "success": true }
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
| ontological.bundle-discovery | Bundle 发现 | 100 | **是** | execution, exploration, audit, orchestration |
| ontological.package-explorer | Package 探索 | 90 | 否 | execution, exploration |
| ontological.entity-schema-inventory | 实体类型盘点 | 80 | 否 | execution, exploration |
| ontological.relation-schema-inventory | 关系类型盘点 | 70 | 否 | execution, exploration |

> `data` 类型特例：bundle-discovery / package-explorer / entity-schema-inventory / relation-schema-inventory 的 `data` 直接是 **lazy 节点列表**（array），每项为 `{data, has_children, suggested_next_call, ...}`，结构见 §4。

---

## 4. 算子详解

### 4.1 ontological.bundle-discovery（Bundle 发现）— 必选

**功能**：列出平台已发布的 Bundle 列表，支持 `scope.bundles` 过滤。返回 lazy 节点（`suggested_next_call=package-explorer`）。

**适用场景**：盘点平台有哪些语义包。

**输出 data**（list，每项 lazy 节点）：

```json
[
  { "data": { "fqn": "metaforge", "name": "MetaForge 语义基座", "owner": "system", "description": "...", "system": true },
    "has_children": true,
    "suggested_next_call": "ontological.package-explorer" }
]
```

### 4.2 ontological.package-explorer（Package 探索）

**功能**：按层级探索 Package 命名空间树（需 `parent_fqn` 锚点，默认不强制）。受 scope.packages 过滤。返回 lazy 节点（`suggested_next_call=entity-schema-inventory`）。

**适用场景**：下钻某个 bundle 下的包命名空间。

**输出 data**（list，每项 lazy 节点）：

```json
[
  { "data": { "fqn": "metaforge:1.0.0.common", "description": "通用业务语义层级", "depth": 0, "bundleVersionFqn": "metaforge:1.0.0" },
    "has_children": false,
    "suggested_next_call": "ontological.entity-schema-inventory" }
]
```

### 4.3 ontological.entity-schema-inventory（实体类型盘点）

**功能**：盘点 EntitySchema 类型清单，含实例数量和关键属性（需 `parent_fqn` 锚点，默认不强制）。返回 lazy 节点（`suggested_next_call=instance-catalog`）。

**适用场景**：确认平台有哪些实体类型、各类型实例规模。

**输出 data**（list，每项 lazy 节点）：

```json
[
  { "data": { "schema": { "fqn": "metaforge:1.0.0.agent.Agent", "name": "Agent", "description": "...", "enabled": true },
              "key_attributes": [], "instance_count": 2 },
    "has_children": true,
    "suggested_next_call": "ontological.instance-catalog",
    "instance_count": 2, "key_attributes": [] }
]
```

### 4.4 ontological.relation-schema-inventory（关系类型盘点）

**功能**：盘点 RelationSchema 类型清单（含 source/target 端点与关联类型）。返回 lazy 节点。

**适用场景**：确认平台有哪些关系类型、谁连谁。

**输出 data**（list，每项 lazy 节点）：

```json
[
  { "data": { "fqn": "metaforge:1.0.0.agent.StepUsesCapability", "name": "StepUsesCapability",
              "description": "步骤执行使用某能力", "associationType": "ASSOCIATION_REFERENCE", "enabled": true },
    "has_children": false }
]
```

> **注意（通用）**：本模板各算子返回 **lazy 节点列表**（`data` + `has_children` + `suggested_next_call`），`suggested_next_call` 提示渐进式下钻的下一步算子。`scope` 会影响发现结果：`scope.bundles` 非空时 bundle-discovery 按 scope 过滤，可能返回空。

---

## 5. 错误场景

| 场景 | 结果 |
|------|------|
| `selectOperators` 含模板未声明算子 | 34014 |
| scope 声明无效 bundle/package/entitySchema | 34003 |
| 模板不存在 | 34001 |

---

## 6. 完整示例

```bash
# 仅展开 Bundle 层
curl -s -X POST http://localhost:8080/api/v1/cognition/DISCOVER \
  -H 'Content-Type: application/json' \
  -d '{
    "scope": {"bundles":["metaforge:1.0.0"],"packages":[],"domainGroups":[],"domains":[],"entitySchemas":[]},
    "params": {"selectOperators":["ontological.bundle-discovery"]},
    "format":"JSON","cognitionDepth":"L3","agentArchetype":"EXECUTION","maxTokens":8000
  }'

# 盘点 agent 包下的关系类型
curl -s -X POST http://localhost:8080/api/v1/cognition/DISCOVER \
  -H 'Content-Type: application/json' \
  -d '{
    "scope": {"bundles":["metaforge:1.0.0"],"packages":[],"domainGroups":[],"domains":[],"entitySchemas":[]},
    "params": {"parent_fqn":"metaforge:1.0.0.agent","selectOperators":["ontological.relation-schema-inventory"]},
    "format":"JSON","cognitionDepth":"L3","agentArchetype":"EXECUTION","maxTokens":8000
  }'
```
