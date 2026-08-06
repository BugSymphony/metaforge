---
id: metaforge-compute-engine.rest-api
protocol: REST
version: 1.0.0
owner: metaforge-compute-engine
description: 语义查询与推理引擎 REST API 契约。统一使用 ApiResponse<T> 格式，通过 @Tag(name = "compute-engine") 分组。
type: business
---

# REST API Contract: metaforge-compute-engine

**Protocol**: RESTful HTTP/1.1 + JSON
**Base Path**: `/api/v1/compute-engine`
**Version**: 1.0.0

> 所有接口统一复用 foundation-core `ApiResponse<T>` 标准响应格式。Controller 通过 `@Tag(name = "compute-engine")` 标注分组，由 foundation-core SpringDoc 自动生成 OpenAPI 文档。

---

## 通用规范

### 请求格式

- `Content-Type`: `application/json;charset=UTF-8`
- 认证方式由 `metaforge-boot` 全局安全配置统一管控，本 BC 不单独处理认证

### 响应格式

所有响应被 foundation-core `GlobalResponseBodyAdvice` 自动包装：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "traceId": "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6"
}
```

### 错误码

本 BC 错误码范围 33000-33999，详见 Application Service 契约错误码表。

---

## API 端点

### 1. 多维图查询

#### 1.1 邻接查询

```
POST /api/v1/compute-engine/adjacency
```

**Request Body**:
```json
{
  "sourceFqn": "order:1.0.0.pkg_order.Order_001",
  "direction": "FORWARD",
  "maxDepth": 3,
  "relationTypes": ["COMPOSITION", "DEPENDENCY_INFLUENCE"],
  "filterCriteria": null
}
```

**Response (200)**:
```json
{
  "entities": [
    {"fqn": "order:1.0.0.pkg_order.Order_001", "name": "订单001", "entitySchemaFqn": "order:1.0.0.pkg_order.Order"}
  ],
  "relations": [
    {"fqn": "Order_001#COMPOSITION#Item_003", "associationType": "COMPOSITION", "sourceEntityFqn": "order:1.0.0.pkg_order.Order_001", "targetEntityFqn": "order:1.0.0.pkg_order.Item_003"}
  ],
  "adjacencyMap": {"order:1.0.0.pkg_order.Order_001": ["Order_001#COMPOSITION#Item_003"]},
  "truncated": false,
  "truncatedReason": null
}
```

**错误**: 404（实体不存在）、422（深度超限）、504（查询超时）

---

#### 1.2 组合层级树查询

```
POST /api/v1/compute-engine/composition-tree
```

**Request Body**:
```json
{
  "rootFqn": "order:1.0.0.pkg_order.Order_001",
  "direction": "BACKWARD",
  "maxDepth": 10,
  "filterCriteria": null
}
```

**Response (200)**: 与邻接查询同结构，`direction=FORWARD` 时为树形嵌套，`BACKWARD` 时为扁平父链列表。

---

#### 1.3 子图提取查询

```
POST /api/v1/compute-engine/subgraph
```

**Request Body**:
```json
{
  "centerFqns": ["order:1.0.0.pkg_order.Order_001"],
  "expandDepth": 2,
  "relationTypes": null,
  "filterCriteria": null
}
```

**Response (200)**: `GraphQueryResult` 结构。

---

#### 1.4 图模式匹配查询

```
POST /api/v1/compute-engine/pattern-match
```

**Request Body**:
```json
{
  "pattern": "* -[?]-> * -[?]-> *",
  "maxResults": 500,
  "filterCriteria": null
}
```

**Response (200)**: `GraphQueryResult` 结构，entities 和 relations 字段包含匹配路径上的所有元素。

---

#### 1.5 多条件复合检索

```
POST /api/v1/compute-engine/search
```

**Request Body**:
```json
{
  "entityTypes": ["order:1.0.0.pkg_order.Order"],
  "attributeConditions": [
    {"field": "status", "value": "active", "matchMode": "EXACT"}
  ],
  "relationTypes": null,
  "logicOperator": "AND",
  "pageRequest": {"page": 1, "size": 20, "sort": "createdTime:desc"},
  "filterCriteria": null
}
```

**Response (200)**: `PageResult<EntitySummary>` 包装在 `ApiResponse<T>.data` 中。

---

#### 1.6 批量语义查询

```
POST /api/v1/compute-engine/batch
```

**Request Body**:
```json
{
  "fqns": ["order:1.0.0.pkg_order.Order_001", "order:1.0.0.pkg_order.Item_003"],
  "filterCriteria": null
}
```

**Response (200)**: `GraphQueryResult` 结构。

**错误**: 400（超限 > 200）

---

### 2. 路径推理

#### 2.1 两点间路径查询

```
POST /api/v1/compute-engine/paths
```

**Request Body**:
```json
{
  "sourceFqn": "order:1.0.0.pkg_order.Order_001",
  "targetFqn": "order:1.0.0.pkg_order.Item_003",
  "direction": "BOTH",
  "relationTypes": null,
  "maxDepth": 5,
  "shortestOnly": false,
  "filterCriteria": null
}
```

**Response (200)**:
```json
{
  "paths": [
    {
      "steps": [
        {"fromEntityFqn": "order:1.0.0.pkg_order.Order_001", "toEntityFqn": "order:1.0.0.pkg_order.Item_003", "relationFqn": "Order_001#COMPOSITION#Item_003", "relationType": "COMPOSITION", "semanticDescription": "订单到订单项的组合包含关系"}
      ],
      "length": 1,
      "totalWeight": 1.0
    }
  ],
  "totalPaths": 1,
  "truncated": false,
  "truncatedReason": null
}
```

---

#### 2.2 传递闭包推理

```
POST /api/v1/compute-engine/closure
```

**Request Body**:
```json
{
  "sourceFqn": "order:1.0.0.pkg_order.Order_001",
  "relationTypes": null,
  "filterCriteria": null
}
```

**Response (200)**: `ClosureResult` 结构。

---

#### 2.3 多跳语义推理

```
POST /api/v1/compute-engine/multi-hop
```

**Request Body**:
```json
{
  "sourceFqn": "order:1.0.0.pkg_order.Order_001",
  "hopSteps": [
    {"relationType": "COMPOSITION", "direction": "FORWARD"},
    {"relationType": "ASSOCIATION_REFERENCE", "direction": "FORWARD"}
  ],
  "filterCriteria": null
}
```

**Response (200)**: `PathResult` 结构。

---

#### 2.4 路径可达性判定

```
POST /api/v1/compute-engine/reachability
```

**Request Body**:
```json
{
  "sourceFqn": "order:1.0.0.pkg_order.Order_001",
  "targetFqn": "order:1.0.0.pkg_order.Item_003",
  "relationTypes": null,
  "maxDepth": 5
}
```

**Response (200)**: `PathResult` 结构（首条路径即返回）。

---

### 3. 影响溯源

#### 3.1 正向影响扩散

```
POST /api/v1/compute-engine/impact/diffuse
```

**Request Body**:
```json
{
  "sourceFqn": "order:1.0.0.pkg_order.Order_001",
  "relationTypes": ["COMPOSITION", "DEPENDENCY_INFLUENCE"],
  "maxDepth": 3,
  "filterCriteria": null
}
```

**Response (200)**: `ImpactTraceResult` 结构。

---

#### 3.2 反向依赖溯源

```
POST /api/v1/compute-engine/impact/trace
```

**Request Body**:
```json
{
  "sourceFqn": "order:1.0.0.pkg_order.Item_003",
  "relationTypes": ["COMPOSITION", "DEPENDENCY_INFLUENCE"],
  "maxDepth": 3,
  "filterCriteria": null
}
```

**Response (200)**: `ImpactTraceResult` 结构。

---

#### 3.3 影响路径详情

```
GET /api/v1/compute-engine/impact/paths?sourceFqn={source}&targetFqn={target}&relationTypes=COMPOSITION&maxDepth=5
```

**Response (200)**: `ImpactTraceResult` 结构。

---

## 通用错误响应

```json
{
  "code": 33001,
  "message": "查询起点实体 FQN 不存在: order:1.0.0.pkg_order.NotExist",
  "data": null,
  "traceId": "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6"
}
```

## 超时处理

所有查询端点设置超时 2000ms（可配置 `metaforge.compute-engine.traversal.timeout-ms`），超时自动中断并返回已获取的部分结果及 `truncated=true, truncatedReason=TIMEOUT`。
