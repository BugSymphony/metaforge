# REST API 契约：metaforge-graph

**Protocol**: REST API (HTTP/1.1 + JSON)
**Module**: `metaforge-graph-core` (Controller 实现)
**Base Path**: `/api/v1/graph`
**Version**: 1.0.0

> 所有 REST API 响应统一由 foundation-core 全局切面包装为 `ApiResponse<T>` 格式，响应包含 `code`、`message`、`data`、`traceId` 四个字段。分页接口使用 `PageRequest` 入参、`PageResult<T>` 出参。OpenAPI 文档由 SpringDoc 自动生成，Controller 通过 `@Tag(name = "语义关系管理")` 分组。

---

## 1. 草稿管理

| 方法 | 路径 | 描述 |
|------|------|------|
| `POST` | `/api/v1/graph/drafts` | 创建关系草稿 |
| `POST` | `/api/v1/graph/drafts/from-active` | 基于生效版本创建草稿 |
| `PUT` | `/api/v1/graph/drafts/{fqn}/content` | 更新草稿内容 |
| `GET` | `/api/v1/graph/drafts/{fqn}` | 查询草稿详情 |
| `DELETE` | `/api/v1/graph/drafts/{fqn}` | 物理删除草稿 |

### POST /api/v1/graph/drafts — 创建关系草稿

**Request Body**:
```json
{
  "sourceEntityFqn": "Order_001",
  "relationTypeFqn": "order:1.0.0.COMPOSITION",
  "targetEntityFqn": "OrderItem_003",
  "name": "订单项组成关系",
  "description": "订单与订单项的组成关联",
  "content": { "quantity": 1 },
  "embedding": null
}
```

**Success Response (200)**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "fqn": "Order_001#order:1.0.0.COMPOSITION#OrderItem_003",
    "name": "订单项组成关系",
    "description": "订单与订单项的组成关联",
    "sourceEntityFqn": "Order_001",
    "targetEntityFqn": "OrderItem_003",
    "relationType": "COMPOSITION",
    "relationSchemaFqn": "order:1.0.0.COMPOSITION",
    "content": { "quantity": 1 },
    "embedding": null,
    "baseVersion": null,
    "createdBy": "admin",
    "createdTime": "2026-08-01 10:00:00",
    "updatedBy": "admin",
    "updatedTime": "2026-08-01 10:00:00"
  },
  "traceId": "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6"
}
```

**Error Codes**: `32001` (FQN 冲突), `32002` (Schema 未发布), `32009` (端点未生效), `32003` (结构校验失败), `32015` (端点类型不匹配)

### PUT /api/v1/graph/drafts/{fqn}/content — 更新草稿内容

**Path Parameter**: `fqn` — 关系草稿 FQN (URL-encoded)

**Request Body**:
```json
{
  "content": { "quantity": 2 },
  "embedding": null
}
```

**Success Response (200)**: 更新后的草稿 DTO

**Error Codes**: `32005` (草稿不存在), `32003` (结构校验失败)

### DELETE /api/v1/graph/drafts/{fqn} — 物理删除草稿

**Path Parameter**: `fqn` — 关系草稿 FQN (URL-encoded)

**Success Response (200)**: `data: null`

**Error Codes**: `32005` (草稿不存在)

---

## 2. 版本生效与下线

| 方法 | 路径 | 描述 |
|------|------|------|
| `POST` | `/api/v1/graph/relations/activate` | 执行草稿生效 |
| `POST` | `/api/v1/graph/relations/deprecate` | 执行关系下线 |
| `POST` | `/api/v1/graph/relations/reactivate` | 重新生效（基于历史版本） |
| `POST` | `/api/v1/graph/relations/check-deprecation` | 校验下线前置条件 |

### POST /api/v1/graph/relations/activate — 执行草稿生效

**Request Body**:
```json
{
  "fqn": "Order_001#order:1.0.0.COMPOSITION#OrderItem_003"
}
```

**Success Response (200)**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "fqn": "Order_001#order:1.0.0.COMPOSITION#OrderItem_003",
    "name": "订单项组成关系",
    "description": "订单与订单项的组成关联",
    "sourceEntityFqn": "Order_001",
    "targetEntityFqn": "OrderItem_003",
    "relationType": "COMPOSITION",
    "relationSchemaFqn": "order:1.0.0.COMPOSITION",
    "content": { "quantity": 2 },
    "embedding": null,
    "currentVersion": 1,
    "createdBy": "admin",
    "createdTime": "2026-08-01 10:00:00",
    "updatedBy": "admin",
    "updatedTime": "2026-08-01 10:01:00"
  },
  "traceId": "..."
}
```

**Error Codes**: `32005` (草稿不存在), `32003` (结构校验失败), `32009` (端点未生效), `32010` (基数违反), `32007` (生效事务失败)

### POST /api/v1/graph/relations/deprecate — 执行关系下线

**Request Body**:
```json
{
  "fqn": "Order_001#order:1.0.0.COMPOSITION#OrderItem_003"
}
```

**Success Response (200)**: `data: null`

**Error Codes**: `32004` (关系不存在), `32008` (依赖阻塞——返回阻塞关系清单)

### POST /api/v1/graph/relations/check-deprecation — 校验下线前置条件

**Request Body**:
```json
{
  "fqn": "Order_001#order:1.0.0.COMPOSITION#OrderItem_003"
}
```

**Success Response (200)**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "canDeprecate": false,
    "blockingRelations": [
      "Product_001#DEPENDENCY_INFLUENCE#Order_001"
    ]
  },
  "traceId": "..."
}
```

---

## 3. 关系查询

| 方法 | 路径 | 描述 |
|------|------|------|
| `GET` | `/api/v1/graph/relations/{fqn}` | FQN 精准查询 |
| `GET` | `/api/v1/graph/relations/outbound` | 实体出边查询 |
| `GET` | `/api/v1/graph/relations/inbound` | 实体入边查询 |
| `GET` | `/api/v1/graph/relations` | 条件列表查询（简便快捷接口） |
| `POST` | `/api/v1/graph/relations/filter` | 多维过滤查询（主接口） |
| `GET` | `/api/v1/graph/admin/relations` | 管理员全状态聚合查询 |

### GET /api/v1/graph/relations/{fqn} — FQN 精准查询

**Path Parameter**: `fqn` — 关系 FQN (URL-encoded)

**Response**: 完整 `RelationInstanceDto`

### GET /api/v1/graph/relations/outbound — 实体出边查询

**Query Parameters**:
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| `entityFqn` | String | 是 | 源实体 FQN |
| `relationType` | String | 否 | 关系类型过滤 |
| `targetEntityType` | String | 否 | 目标实体类型过滤 |

**Response**: `List<RelationInstanceDto>`

### GET /api/v1/graph/relations/inbound — 实体入边查询

**Query Parameters**:
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| `entityFqn` | String | 是 | 目标实体 FQN |
| `relationType` | String | 否 | 关系类型过滤 |
| `sourceEntityType` | String | 否 | 源实体类型过滤 |

**Response**: `List<RelationInstanceDto>`

### POST /api/v1/graph/relations/filter — 多维过滤查询

**Request Body** (`RelationQueryRequest`):
```json
{
  "relationTypes": ["COMPOSITION", "DEPENDENCY_INFLUENCE"],
  "sourceEntityTypes": null,
  "targetEntityTypes": null,
  "sourceEntityFqns": null,
  "targetEntityFqns": ["OrderItem_"],
  "relationSchemaFqns": ["order:1.0.0.COMPOSITION"],
  "nameKeyword": "组成",
  "descriptionKeyword": null,
  "createdAtStart": null,
  "createdAtEnd": null,
  "updatedAtStart": null,
  "updatedAtEnd": null,
  "pageRequest": {
    "page": 1,
    "size": 20,
    "sort": "updatedTime:desc"
  }
}
```

**Success Response (200)**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [ ... ],
    "total": 42,
    "page": 1,
    "size": 20,
    "totalPages": 3
  },
  "traceId": "..."
}
```

### GET /api/v1/graph/relations — 条件列表查询（简便快捷接口）

**Query Parameters**:
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| `fqnPrefix` | String | 否 | FQN 前缀 |
| `relationSchemaFqn` | String | 否 | RelationSchema FQN |
| `page` | int | 否 | 页码（默认 1） |
| `size` | int | 否 | 每页条数（默认 20） |
| `sort` | String | 否 | 排序字段（格式 `field:asc\|desc`） |

**Response**: `PageResult<RelationInstanceDto>`

### GET /api/v1/graph/admin/relations — 管理员全状态聚合查询

**需管理员权限**

**Query Parameters**: 同管理端查询 DTO `AdminQueryRequest` 的参数

**Response**: `PageResult<RelationInstanceDto>`（每条数据标注状态与来源表）

---

## 4. 历史追溯

| 方法 | 路径 | 描述 |
|------|------|------|
| `GET` | `/api/v1/graph/versions/{fqn}` | 查询全历史版本列表 |
| `GET` | `/api/v1/graph/versions/{fqn}/{version}` | 查询单版本详情 |
| `POST` | `/api/v1/graph/versions/diff` | 两版本差异对比 |

### GET /api/v1/graph/versions/{fqn} — 查询全历史版本列表

**Response**: `List<RelationVersionDto>`（按版本号倒序）

### GET /api/v1/graph/versions/{fqn}/{version} — 查询单版本详情

**Response**: `RelationVersionDto`（含 content 全量快照）

### POST /api/v1/graph/versions/diff — 两版本差异对比

**Request Body**:
```json
{
  "fqn": "Order_001#order:1.0.0.COMPOSITION#OrderItem_003",
  "versionA": 1,
  "versionB": 3
}
```

**Response**: `VersionDiffDto`（新增/修改/删除字段分类）

---

## 5. 批量导入导出

| 方法 | 路径 | 描述 |
|------|------|------|
| `POST` | `/api/v1/graph/import` | 批量导入（YAML/JSON） |
| `POST` | `/api/v1/graph/export` | 按条件导出 |

### POST /api/v1/graph/import — 批量导入

**Request Body**:
```json
{
  "content": "...",
  "format": "YAML",
  "strategy": "SKIP"
}
```

**Response**: `ImportResultDto`

### POST /api/v1/graph/export — 按条件导出

**Request Body**:
```json
{
  "fqnPrefixes": ["Order_001#"],
  "relationTypes": null,
  "fqns": null,
  "format": "JSON"
}
```

**Response**: `ExportResultDto`

---

## 6. 拓扑查询

| 方法 | 路径 | 描述 |
|------|------|------|
| `GET` | `/api/v1/graph/topology/dependent-relations` | 查询实体关联依赖关系 |
| `POST` | `/api/v1/graph/topology/validate` | 批量拓扑完整性校验 |
| `GET` | `/api/v1/graph/topology/relation-count` | 查询实体关系计数 |

### GET /api/v1/graph/topology/dependent-relations

**Query Parameter**: `entityFqn` — 实体 FQN

**Response**: `List<String>`（DEPENDENCY_INFLUENCE 类型的关系 FQN 列表）

### POST /api/v1/graph/topology/validate — 拓扑完整性校验

**Request Body**:
```json
{
  "fqnPrefix": "Order_001#",
  "relationType": "COMPOSITION"
}
```

**Response**: `TopologyValidationReport`

---

## HTTP 状态码映射

| HTTP Status | 典型场景 |
|-------------|----------|
| 200 | 所有成功操作 |
| 400 | 请求体/参数解析失败、FQN 解析错误、导入解析失败 |
| 403 | 跨域关系未经授权 |
| 404 | 生效关系/草稿/历史版本不存在 |
| 409 | FQN 冲突、重复草稿、依赖阻塞 |
| 422 | 结构校验失败、端点未生效、基数违反、端点类型不匹配、Schema 未发布 |
| 500 | 原子事务执行失败、数据库异常 |
