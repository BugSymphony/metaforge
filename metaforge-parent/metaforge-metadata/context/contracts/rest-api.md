---
id: metadata-management.rest-api
protocol: REST
version: 1.0.0
owner: metadata-management
description: 元数据管理 BC 对外暴露的 REST API 契约。覆盖草稿管理、版本生效/下线、查询检索、历史追溯、批量导入导出六大能力域。所有响应统一使用 foundation-core 定义的 ApiResponse<T> 格式。
type: business
---

# REST API Contract: metadata-management

**Base URL**: `/api/v1/metadata`
**Content-Type**: `application/json;charset=UTF-8`
**Response Format**: 统一 `ApiResponse<T>` 包装（遵循 foundation-core REST API Contract）

---

## 资源端点概览

| 资源 | HTTP 方法 | 端点 | 描述 | 优先级 |
|------|----------|------|------|--------|
| 草稿 | `POST` | `/drafts` | 创建草稿 | P1 |
| 草稿 | `GET` | `/drafts/{fqn}` | 查询草稿 | P1 |
| 草稿 | `PUT` | `/drafts/{fqn}/content` | 更新草稿内容 | P1 |
| 草稿 | `DELETE` | `/drafts/{fqn}` | 删除草稿 | P1 |
| 草稿 | `POST` | `/drafts/from-active/{fqn}` | 从生效版本创建修改草稿 | P1 |
| 生效 | `POST` | `/entities/{fqn}/activate` | 草稿生效 | P1 |
| 生效 | `POST` | `/entities/{fqn}/deactivate` | 下线生效版本 | P1 |
| 生效 | `POST` | `/entities/{fqn}/reactivate` | 从历史版本重新生效 | P1 |
| 生效 | `GET` | `/entities/{fqn}/deactivation-check` | 下线前置条件校验 | P1 |
| 查询 | `GET` | `/entities/{fqn}` | FQN 精准查询 | P2 |
| 查询 | `GET` | `/entities` | 多条件组合查询 | P2 |
| 查询 | `GET` | `/entities/query/fqn-prefix` | FQN 前缀范围查询 | P2 |
| 查询 | `GET` | `/entities/query/entity-schema` | 按元模型类型查询 | P2 |
| 查询 | `GET` | `/admin/metadata` | 管理员全状态查询 | P2 |
| 历史 | `GET` | `/history/{fqn}/versions` | 全历史版本列表 | P2 |
| 历史 | `GET` | `/history/{fqn}/versions/{version}` | 单版本详情 | P2 |
| 历史 | `POST` | `/history/diff` | 版本差异对比 | P2 |
| 导入导出 | `POST` | `/import` | 批量导入 | P3 |
| 导入导出 | `POST` | `/export` | 批量导出 | P3 |

---

## 1. 草稿管理

### POST `/drafts` — 创建草稿

**请求体**:
```json
{
  "fqn": "SalesOrder_001",
  "name": "销售订单",
  "description": "标准销售订单元数据定义",
  "parentFqn": null,
  "entitySchemaFqn": "order:1.0.0.pkg_order.Order",
  "content": {
    "orderId": "SO-123456",
    "customerName": "张三",
    "status": "active"
  },
  "embedding": null
}
```

**成功响应** (201 Created):
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "fqn": "SalesOrder_001",
    "name": "销售订单",
    "description": "标准销售订单元数据定义",
    "parentFqn": null,
    "entitySchemaFqn": "order:1.0.0.pkg_order.Order",
    "content": {
      "orderId": "SO-123456",
      "customerName": "张三",
      "status": "active"
    },
    "embedding": [0.1, 0.2, 0.3],
    "baseVersion": null,
    "createdBy": "system",
    "createdTime": "2026-08-01 14:30:00",
    "updatedBy": "system",
    "updatedTime": "2026-08-01 14:30:00"
  },
  "traceId": "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6"
}
```

**校验失败响应** (422):
```json
{
  "code": 31003,
  "message": "JSON Schema 校验失败: 字段 'orderId' 不符合正则 '^SO-\\d{6}$'",
  "data": null,
  "traceId": "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6"
}
```

**FQN 冲突响应** (409):
```json
{
  "code": 31001,
  "message": "FQN 'SalesOrder_001' 已存在（主表或草稿表）",
  "data": null,
  "traceId": "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6"
}
```

### GET `/drafts/{fqn}` — 查询草稿

**响应** (200):
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "fqn": "SalesOrder_001",
    "name": "销售订单",
    "entitySchemaFqn": "order:1.0.0.pkg_order.Order",
    "content": { "orderId": "SO-123456" },
    "baseVersion": null,
    "createdBy": "system",
    "createdTime": "2026-08-01 14:30:00",
    "updatedBy": "system",
    "updatedTime": "2026-08-01 14:30:00"
  },
  "traceId": "..."
}
```

### PUT `/drafts/{fqn}/content` — 更新草稿内容

**请求体**:
```json
{
  "content": {
    "orderId": "SO-123456",
    "customerName": "李四",
    "status": "active",
    "priority": "high"
  }
}
```

**响应** (200): 与 GET 结构一致，展示更新后的内容。

### DELETE `/drafts/{fqn}` — 删除草稿

**响应** (200):
```json
{
  "code": 200,
  "message": "success",
  "data": null,
  "traceId": "..."
}
```

### POST `/drafts/from-active/{fqn}` — 从生效版本创建修改草稿

**说明**: 无请求体，从主表复制生效版本内容，base_version 记录原版本号。

---

## 2. 版本生效与下线

### POST `/entities/{fqn}/activate` — 草稿生效

**说明**: 无请求体。原子事务：主表写入 + 历史表归档 + 草稿表删除。

**成功响应** (200):
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "fqn": "SalesOrder_001",
    "name": "销售订单",
    "entitySchemaFqn": "order:1.0.0.pkg_order.Order",
    "content": { "orderId": "SO-123456" },
    "currentVersion": 1,
    "createdBy": "system",
    "createdTime": "2026-08-01 14:30:00",
    "updatedBy": "system",
    "updatedTime": "2026-08-01 14:35:00"
  },
  "traceId": "..."
}
```

### POST `/entities/{fqn}/deactivate` — 下线生效版本

**前置条件失败响应** (409):
```json
{
  "code": 31008,
  "message": "下线被拦截: 存在生效子实体 [SalesOrder_001.OrderItem_005]; 存在活跃引用 [OrderReport_010]",
  "data": {
    "canDeactivate": false,
    "activeReferences": ["OrderReport_010"],
    "activeChildren": ["SalesOrder_001.OrderItem_005"]
  },
  "traceId": "..."
}
```

### POST `/entities/{fqn}/reactivate` — 从历史版本重新生效

**说明**: 无请求体。从历史表恢复最新归档版本到主表，不新增历史记录。

### GET `/entities/{fqn}/deactivation-check` — 下线前置条件校验

**响应** (200):
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "canDeactivate": true,
    "activeReferences": [],
    "activeChildren": []
  },
  "traceId": "..."
}
```

---

## 3. 查询检索

### GET `/entities/{fqn}` — FQN 精准查询

**响应** (200): 返回完整元数据 DTO（含 content 全量字段）。

**不存在响应** (404):
```json
{
  "code": 31004,
  "message": "元数据实体 'SalesOrder_001' 不存在或已下线",
  "data": null,
  "traceId": "..."
}
```

### GET `/entities` — 多条件组合查询

**查询参数**:
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| `fqnPrefixes` | `List<String>` | 否 | FQN 前缀集合（逗号分隔，OR 并集） |
| `entitySchemaFqn` | `String` | 否 | 元模型类型 FQN |
| `fields` | `List<String>` | 否 | 属性条件字段名列表 |
| `values` | `List<String>` | 否 | 属性条件值列表（与 fields 顺序对应） |
| `matchMode` | `String` | 否 | `EXACT` 或 `PREFIX`，默认 `EXACT` |
| `page` | `int` | 否 | 页码，默认 1 |
| `size` | `int` | 否 | 每页条数，默认 20 |
| `sort` | `String` | 否 | 排序字段，如 `updatedTime:desc` |

**响应** (200):
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "fqn": "SalesOrder_001",
        "name": "销售订单",
        "entitySchemaFqn": "order:1.0.0.pkg_order.Order",
        "currentVersion": 1,
        "updatedTime": "2026-08-01 14:35:00"
      }
    ],
    "total": 1,
    "page": 1,
    "size": 20,
    "totalPages": 1
  },
  "traceId": "..."
}
```

### GET `/entities/query/fqn-prefix` — FQN 前缀范围查询

**查询参数**: `prefixes` (逗号分隔的 FQN 前缀列表), `page`, `size`, `sort`

### GET `/entities/query/entity-schema` — 按元模型类型查询

**查询参数**: `entitySchemaFqn` (必填), `page`, `size`, `sort`

### GET `/admin/metadata` — 管理员全状态聚合查询

**查询参数**: `statuses` (可选，逗号分隔：DRAFT,ACTIVE,HISTORY), `fqnPrefix`, `entitySchemaFqn`, `page`, `size`, `sort`

**响应**: 每条数据额外包含 `status` 字段（`DRAFT` / `ACTIVE` / `HISTORY`）。

---

## 4. 历史版本追溯

### GET `/history/{fqn}/versions` — 全历史版本列表

**响应** (200):
```json
{
  "code": 200,
  "message": "success",
  "data": [
    { "fqn": "SalesOrder_001", "version": 3, "createdBy": "system", "createdTime": "2026-08-01 15:00:00" },
    { "fqn": "SalesOrder_001", "version": 2, "createdBy": "system", "createdTime": "2026-08-01 14:45:00" },
    { "fqn": "SalesOrder_001", "version": 1, "createdBy": "system", "createdTime": "2026-08-01 14:35:00" }
  ],
  "traceId": "..."
}
```

### GET `/history/{fqn}/versions/{version}` — 单版本详情

**响应** (200): 返回完整 `EntityVersionDto`，含 content 全量快照。

### POST `/history/diff` — 版本差异对比

**请求体**:
```json
{
  "fqn": "SalesOrder_001",
  "versionA": 1,
  "versionB": 3
}
```

**响应** (200):
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "fqn": "SalesOrder_001",
    "versionA": 1,
    "versionB": 3,
    "addedFields": [
      { "fieldPath": "status", "oldValue": null, "newValue": "active" }
    ],
    "modifiedFields": [
      { "fieldPath": "name", "oldValue": "A", "newValue": "B" }
    ],
    "deletedFields": []
  },
  "traceId": "..."
}
```

---

## 5. 批量导入导出

### POST `/import` — 批量导入

**请求头**: `Content-Type: multipart/form-data` 或 `application/json`

**JSON 请求体**:
```json
{
  "content": "[{\"fqn\":\"SO_001\",\"name\":\"订单1\",\"entitySchemaFqn\":\"order:1.0.0.pkg_order.Order\",\"content\":{\"orderId\":\"SO-000001\"}}]",
  "format": "JSON",
  "strategy": "SKIP"
}
```

**响应** (200):
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalCount": 10,
    "successCount": 8,
    "skipCount": 2,
    "failureCount": 0,
    "items": [
      { "fqn": "SO_001", "success": true, "errorMessage": null },
      { "fqn": "SO_002", "success": false, "errorMessage": "FQN 已存在，跳过" }
    ]
  },
  "traceId": "..."
}
```

### POST `/export` — 批量导出

**请求体**:
```json
{
  "type": "FQN_PREFIXES",
  "fqnPrefixes": ["SalesOrder_"],
  "entitySchemaFqn": null,
  "fqns": null,
  "format": "JSON"
}
```

**响应** (200):
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalCount": 5,
    "content": "[{\"fqn\":\"SalesOrder_001\",\"name\":\"...\",\"content\":{...}}]",
    "format": "JSON"
  },
  "traceId": "..."
}
```

**导出粒度类型** (`ExportRequestDto.type`):
- `FQN_PREFIXES` — 按 FQN 前缀范围
- `ENTITY_SCHEMA` — 按元模型类型
- `FQN_LIST` — 按指定 FQN 列表

---

## 错误响应对照

BC 专用错误码范围：**31000-31099**。

| 错误码 | HTTP 状态 | 触发场景 |
|--------|----------|---------|
| 31001 | 409 Conflict | FQN 重复冲突 |
| 31002 | 400 Bad Request | FQN segment 不合法 |
| 31003 | 422 Unprocessable Entity | JSON Schema 校验失败 |
| 31004 | 404 Not Found | 元数据不存在或已下线 |
| 31005 | 404 Not Found | 草稿不存在 |
| 31006 | 404 Not Found | 历史版本不存在 |
| 31007 | 500 Internal Server Error | 生效事务失败 |
| 31008 | 409 Conflict | 下线被拦截 |
| 31009 | 422 Unprocessable Entity | 父实体未生效 |
| 31010 | 422 Unprocessable Entity | 元模型版本未发布 |
| 31011 | 400 Bad Request | 导入文件解析失败 |
| 31012 | 400 Bad Request | FQN segment 含保留分隔符 |
